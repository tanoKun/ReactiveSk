package com.github.tanokun.addon.runtime.skript.init

import ch.njol.skript.Skript
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionList
import ch.njol.skript.lang.ExpressionType
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.skript.util.LiteralUtils
import ch.njol.util.Kleenean
import com.github.tanokun.addon.definition.dynamic.DynamicClass
import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.dynamicClassDefinitionLoader
import com.github.tanokun.addon.dynamicJavaClassLoader
import com.github.tanokun.addon.runtime.skript.init.mediator.RuntimeConstructorMediator
import org.bukkit.event.Event
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException

@Suppress("UNCHECKED_CAST")
class NewInstanceExpression: SimpleExpression<Any>() {
    companion object {
        init {
            val classes = dynamicClassDefinitionLoader.getClassNames().map { it.identifier }

            Skript.registerExpression(
                NewInstanceExpression::class.java,
                Any::class.java,
                ExpressionType.SIMPLE,
                "create (${classes.reduceOrNull { acc, className -> "$acc|$className" }}) [with %objects%]"
            )
        }
    }

    private lateinit var className: Identifier
    private lateinit var argumentExprs: Array<Expression<Any>>

    private lateinit var dynamicClass: Class<out DynamicClass>

    private lateinit var constructor: Constructor<out DynamicClass>

    override fun init(
        exprs: Array<out Expression<*>>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult,
    ): Boolean {
        val argumentsExpr: Expression<Any> = LiteralUtils.defendExpression(exprs[0])

        this.argumentExprs =
            if (argumentsExpr is ExpressionList<*>)
                (argumentsExpr as ExpressionList<Any>).getExpressions() as Array<Expression<Any>>
            else arrayOf(argumentsExpr)

        className = Identifier(parseResult.expr.split(" ", limit = 3)[1])
        dynamicClass = dynamicJavaClassLoader.getDynamicClassOrNull(className) ?: let {
            Skript.error("Cannot find dynamic class '$className'.")
            return false
        }

        val argumentTypes = argumentExprs.map(Expression<Any>::getReturnType).toTypedArray()

        try {
            constructor = dynamicClass.getConstructor(RuntimeConstructorMediator::class.java, *argumentTypes)
        } catch (_: NoSuchMethodException) {
            val argLine = argumentTypes.map { it.simpleName }.reduceOrNull { acc, type -> "$acc, $type" }
            Skript.error("Cannot find constructor with '${argLine}' in '$className'.")
            return false
        }

        return true
    }

    override fun get(e: Event): Array<Any>? {
        val mediator = RuntimeConstructorMediator()
        val arguments = argumentExprs.mapNotNull { if (it.isSingle) it.getSingle(e) else arrayListOf(*it.getArray(e)) }.toTypedArray()

        try {
            return arrayOf(constructor.newInstance(mediator, *arguments))
        } catch (e: InvocationTargetException) {
            Skript.error("Failed to create instance of '$className' with arguments '${arguments.reduceOrNull { acc, any -> "$acc, $any" }}' -> ${e.targetException.message}.")
        }

        return null
    }

    override fun isSingle(): Boolean = true

    override fun getReturnType() = dynamicClass

    override fun toString(e: Event?, debug: Boolean): String = "create $className with ${argumentExprs.map { it.toString(e, debug) }.reduceOrNull { acc, s -> "$acc, $s" }}"
}
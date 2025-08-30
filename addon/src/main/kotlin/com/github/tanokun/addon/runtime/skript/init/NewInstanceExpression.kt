package com.github.tanokun.addon.runtime.skript.init

import ch.njol.skript.Skript
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionList
import ch.njol.skript.lang.ExpressionType
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.skript.util.LiteralUtils
import ch.njol.util.Kleenean
import com.github.tanokun.addon.definition.DynamicClassInfo
import com.github.tanokun.addon.lookup
import com.github.tanokun.addon.runtime.MethodHandleInvokerUtil
import com.github.tanokun.addon.runtime.skript.init.mediator.RuntimeConstructorMediator
import org.bukkit.event.Event
import java.lang.invoke.MethodType
import java.lang.reflect.InvocationTargetException

@Suppress("UNCHECKED_CAST")
class NewInstanceExpression: SimpleExpression<Any>() {
    companion object {
        init {
            Skript.registerExpression(
                NewInstanceExpression::class.java,
                Any::class.java,
                ExpressionType.SIMPLE,
                "create %*dynamicclassinfo% [with %-objects%]"
            )
        }
    }

    private lateinit var argumentExprs: Array<Expression<Any>>

    private lateinit var dynamicClassInfo: DynamicClassInfo

    private lateinit var methodHandle: (Array<*>, RuntimeConstructorMediator) -> Any

    override fun init(
        exprs: Array<out Expression<*>>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult,
    ): Boolean {
        val argumentsExpr: Expression<Any> = LiteralUtils.defendExpression(exprs[1])

        this.argumentExprs =
            if (argumentsExpr is ExpressionList<*>)
                (argumentsExpr as ExpressionList<Any>).getExpressions() as Array<Expression<Any>>
            else arrayOf(argumentsExpr)

        dynamicClassInfo = (exprs[0] as? Expression<DynamicClassInfo>)?.getSingle(null) ?: let {
            Skript.error("Cannot find dynamic class ${exprs[0]}.")
            return false
        }

        val argumentTypes = argumentExprs.map(Expression<Any>::getReturnType).toTypedArray()
        try {
            val constructorHandle = lookup.findConstructor(
                dynamicClassInfo.classInfo,
                MethodType.methodType(Void.TYPE, arrayOf(RuntimeConstructorMediator::class.java, *argumentTypes))
            ).asType(MethodType.methodType(Any::class.java, Array(argumentTypes.size + 1) { Any::class.java }))

            methodHandle = MethodHandleInvokerUtil.buildConstructor(argumentTypes.size, constructorHandle)
        } catch (_: NoSuchMethodException) {
            val argLine = argumentTypes.map { it.simpleName }.reduceOrNull { acc, type -> "$acc, $type" }
            Skript.error("Cannot find constructor with (${argLine}) in ${exprs[0]}.")
            return false
        }

        return true
    }

    override fun get(e: Event): Array<Any>? {
        val arguments = argumentExprs.mapNotNull { if (it.isSingle) it.getSingle(e) else arrayListOf(*it.getArray(e)) }.toTypedArray()

        try {
            return arrayOf(methodHandle(arguments, RuntimeConstructorMediator()))
        } catch (e: InvocationTargetException) {
            Skript.error("Failed to create instance of '${dynamicClassInfo.classInfo.simpleName}' with arguments (${arguments.reduceOrNull { acc, any -> "$acc, $any" }}) -> ${e.targetException.message}.")
        }

        return null
    }

    override fun isSingle(): Boolean = true

    override fun getReturnType() = dynamicClassInfo.classInfo

    override fun toString(e: Event?, debug: Boolean): String = "create ${dynamicClassInfo.classInfo.simpleName} with ${argumentExprs.map { it.toString(e, debug) }.reduceOrNull { acc, s -> "$acc, $s" }}"
}
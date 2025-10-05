package com.github.tanokun.reactivesk.v263.skript.runtime.instantiation

import ch.njol.skript.Skript
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionList
import ch.njol.skript.lang.ExpressionType
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.skript.util.LiteralUtils
import ch.njol.util.Kleenean
import com.github.tanokun.reactivesk.v263.AmbiguousVariableFrames
import com.github.tanokun.reactivesk.v263.ReactiveSkAddon.Companion.constructorCallers
import com.github.tanokun.reactivesk.v263.caller.method.ConstructorCaller
import com.github.tanokun.reactivesk.v263.skript.DynamicClassInfo
import com.github.tanokun.reactivesk.v263.skript.runtime.instantiation.mediator.RuntimeConstructorMediator
import org.bukkit.event.Event
import java.lang.reflect.InvocationTargetException

@Suppress("UNCHECKED_CAST")
class InstantiationExpression: SimpleExpression<Any>() {
    companion object {
        fun register() {
            Skript.registerExpression(
                InstantiationExpression::class.java,
                Any::class.java,
                ExpressionType.SIMPLE,
                "(new | create) %*dynamicclassinfo% [with %-objects%]",
                "(new | create) %*dynamicclassinfo%\\([%-objects%]\\)"
            )
        }
    }

    private lateinit var argumentExprs: Array<Expression<Any>>

    private lateinit var dynamicClassInfo: DynamicClassInfo

    private lateinit var constructorCaller: ConstructorCaller

    override fun init(
        exprs: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult,
    ): Boolean {
        val argumentsExpr: Expression<Any> = LiteralUtils.defendExpression(exprs[1] ?: ExpressionList(emptyArray(), Any::class.java, true))

        argumentExprs =
            if (argumentsExpr is ExpressionList<*>)
                (argumentsExpr as ExpressionList<Any>).getExpressions() as Array<Expression<Any>>
            else arrayOf(argumentsExpr)

        argumentExprs.forEach {
            if (!it.isSingle) {
                Skript.error("Cannot use $it. It must be single value expression.")
                return false
            }
        }

        dynamicClassInfo = (exprs[0] as? Expression<DynamicClassInfo>)?.getSingle(null) ?: let {
            Skript.error("Cannot find dynamic class ${exprs[0]}.")
            return false
        }

        val argumentTypes = argumentExprs.map(Expression<Any>::getReturnType).toTypedArray()

        val constructor = dynamicClassInfo.clazz
            .declaredConstructors
            .filter { it.parameterCount - 1 == argumentTypes.size }
            .firstOrNull {
                it.parameters
                    .drop(1)
                    .filterIndexed { index, param -> !param.type.isAssignableFrom(argumentTypes[index]) }
                    .isEmpty()
            }

        if (constructor == null) {
            val argLine = argumentTypes.map { it.simpleName }.reduceOrNull { acc, type -> "$acc, $type" }
            Skript.error("Cannot find constructor with (${argLine ?: ""}) in ${dynamicClassInfo.classDefinition.className}.")
            return false
        }

        constructorCaller = constructorCallers.get(constructor) ?: let {
            Skript.error("Internal error: ConstructorCaller for constructor with (${argumentTypes.map { it.simpleName }.reduceOrNull { acc, type -> "$acc, $type" }}) in ${dynamicClassInfo.classDefinition.className} is not found.")
            return false
        }

        return true
    }

    override fun get(e: Event): Array<Any>? {
        val arguments = Array(argumentExprs.size) { i -> argumentExprs[i].getSingle(e) }

        val mediator = RuntimeConstructorMediator()

        try {
            return arrayOf(constructorCaller.call(mediator, *arguments))
        } catch (e: InvocationTargetException) {
            Skript.error("Failed to create instance of '${dynamicClassInfo.clazz.simpleName}' with arguments (${arguments.reduceOrNull { acc, any -> "$acc, $any" }}) -> ${e.targetException.message}.")
        } finally {
            AmbiguousVariableFrames.endFrame(mediator)
        }

        return null
    }

    override fun isSingle(): Boolean = true

    override fun getReturnType() = dynamicClassInfo.clazz

    override fun toString(e: Event?, debug: Boolean): String = "create ${dynamicClassInfo.clazz.simpleName} with ${argumentExprs.map { it.toString(e, debug) }.reduceOrNull { acc, s -> "$acc, $s" }}"
}
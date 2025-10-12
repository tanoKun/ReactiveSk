package com.github.tanokun.reactivesk.v263.skript.runtime.invoke.function.call

import ch.njol.skript.Skript
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionType
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.util.Kleenean
import com.github.tanokun.reactivesk.v263.caller.method.MethodCaller
import com.github.tanokun.reactivesk.v263.skript.runtime.invoke.function.call.NonSuspendCallFunction.callFunction
import com.github.tanokun.reactivesk.v263.skript.runtime.invoke.function.mediator.NonSuspendRuntimeFunctionMediator
import org.bukkit.event.Event

@Suppress("UNCHECKED_CAST")
class NonSuspendCallFunctionExpression : SimpleExpression<Any>() {

    companion object {
        fun register() {
            Skript.registerExpression(
                NonSuspendCallFunctionExpression::class.java, Any::class.java, ExpressionType.SIMPLE,
                "%object% (-> | then) %identifier%\\([%-objects%]\\)"
            )
        }
    }

    private lateinit var targetExpr: Expression<Any>
    private lateinit var methodCaller: MethodCaller
    private lateinit var argumentExprs: Array<Expression<Any>>
    private lateinit var functionReturnType: Class<*>

    override fun init(
        exprs: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult,
    ): Boolean {
        val targetExprIndex = 0
        val functionNameExprIndex = 1

        val callFunction = NonSuspendCallFunction.load(exprs, targetExprIndex, functionNameExprIndex, parser) ?: return false

        this.targetExpr = callFunction.targetExpr
        this.methodCaller = callFunction.methodCaller
        this.argumentExprs = callFunction.argumentExprs
        this.functionReturnType = callFunction.functionReturnType

        return true
    }

    override fun get(e: Event): Array<Any?> {
        val nonSuspendFunction = NonSuspendRuntimeFunctionMediator()
        callFunction(e, targetExpr, methodCaller, argumentExprs, nonSuspendFunction)

        val returnValue = nonSuspendFunction.nonSuspendGetReturn()
        return if (returnValue != null) arrayOf(returnValue) else emptyArray()
    }

    override fun toString(e: Event?, debug: Boolean): String = "call function in ${targetExpr.toString(e, debug)} with ${argumentExprs.map { it.toString(e, debug) }.reduceOrNull { acc, s -> "$acc, $s" }}"

    override fun isSingle(): Boolean = true

    override fun getReturnType() = functionReturnType
}
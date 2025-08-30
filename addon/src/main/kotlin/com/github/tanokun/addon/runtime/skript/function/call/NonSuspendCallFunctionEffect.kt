package com.github.tanokun.addon.runtime.skript.function.call

import ch.njol.skript.Skript
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionList
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.util.LiteralUtils
import ch.njol.util.Kleenean
import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.intermediate.generator.internalFunctionReturnTypeField
import com.github.tanokun.addon.lookup
import com.github.tanokun.addon.runtime.MethodHandleInvokerUtil
import com.github.tanokun.addon.runtime.skript.function.call.mediator.NonSuspendRuntimeFunctionMediator
import com.github.tanokun.addon.runtime.skript.function.call.mediator.RuntimeFunctionMediator
import com.github.tanokun.addon.runtime.variable.VariableFrames
import org.bukkit.event.Event
import java.lang.invoke.MethodType

@Suppress("UNCHECKED_CAST")
class NonSuspendCallFunctionEffect : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                NonSuspendCallFunctionEffect::class.java,
                "call %*identifier% in %object% [with %-objects%]"
            )
        }
    }

    private lateinit var targetExpr: Expression<Any>

    private lateinit var methodHandle: (Array<*>, Any, NonSuspendRuntimeFunctionMediator) -> Unit

    private lateinit var argumentExprs: Array<Expression<Any>>

    private lateinit var functionReturnType: Class<*>

    override fun init(
        exprs: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult,
    ): Boolean {
        val targetExprIndex = 1 //if (matchedPattern == 0) 0 else 1
        val functionNameExprIndex = 0 //if (matchedPattern == 0) 1 else 0

        targetExpr = LiteralUtils.defendExpression(exprs[targetExprIndex])

        val funcName = (exprs[functionNameExprIndex] as Expression<Identifier>).getSingle(null) ?: let {
            Skript.error("Function name is not specified. ${exprs[1]}")
            return false
        }

        val argumentsExpr: Expression<Any> = LiteralUtils.defendExpression(exprs[2] ?: ExpressionList(arrayOf(), Any::class.java, false))

        this.argumentExprs =
            if (argumentsExpr is ExpressionList<*>)
                (argumentsExpr as ExpressionList<Any>).expressions as Array<Expression<Any>>
            else arrayOf(argumentsExpr)

        val argumentTypes = argumentExprs.map(Expression<Any>::getReturnType).toTypedArray()
        val calledClass = targetExpr.getReturnType()

        try {
            val methodHandle = lookup.findVirtual(
                calledClass,
                funcName.identifier,
                MethodType.methodType(Void.TYPE, arrayOf(RuntimeFunctionMediator::class.java, *argumentTypes))
            ).asType(MethodType.methodType(Void.TYPE, Array(argumentTypes.size + 2) { Any::class.java }))

            this.methodHandle = MethodHandleInvokerUtil.buildFunction(argumentTypes.size, methodHandle)
        } catch (_: NoSuchMethodException) {
            Skript.error("Cannot find function '$funcName' in '${calledClass.simpleName}'.")
            return false
        }

        functionReturnType = targetExpr.getReturnType().getField(internalFunctionReturnTypeField(funcName.identifier)).get(null) as Class<*>

        return true
    }

    override fun execute(e: Event) {
        val nonSuspendFunction = NonSuspendRuntimeFunctionMediator()
        val target = targetExpr.getSingle(e) ?: throw IllegalStateException("Target is null.")

        val n = argumentExprs.size
        val args = arrayOfNulls<Any>(n)
        for (i in 0 until n) { args[i] = argumentExprs[i].getSingle(e) }

        try {
            methodHandle(args, target, nonSuspendFunction)
        } catch (e: Throwable) {
            e.printStackTrace()
        } finally {
            VariableFrames.endFrame(nonSuspendFunction)
        }
    }



    override fun toString(e: Event?, debug: Boolean): String? = "await call"
}

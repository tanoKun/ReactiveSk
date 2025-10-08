package com.github.tanokun.reactivesk.v263.skript.runtime.invoke.function.call

import ch.njol.skript.Skript
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionList
import ch.njol.skript.lang.parser.ParserInstance
import ch.njol.skript.util.LiteralUtils
import com.github.tanokun.reactivesk.compiler.backend.codegen.util.internalFunctionNameOf
import com.github.tanokun.reactivesk.compiler.backend.metadata.ModifierMetadata
import com.github.tanokun.reactivesk.lang.Identifier
import com.github.tanokun.reactivesk.v263.AmbiguousVariableFrames
import com.github.tanokun.reactivesk.v263.ReactiveSkAddon.Companion.methodCallers
import com.github.tanokun.reactivesk.v263.caller.method.MethodCaller
import com.github.tanokun.reactivesk.v263.skript.runtime.DynamicAccessChecks.checkAccessError
import com.github.tanokun.reactivesk.v263.skript.runtime.SkriptExpressionInitChecks.checkSingletonError
import com.github.tanokun.reactivesk.v263.skript.runtime.invoke.function.mediator.RuntimeFunctionMediator
import org.bukkit.event.Event

data class CallFunction(
    val targetExpr: Expression<Any>,
    val methodCaller: MethodCaller,
    val argumentExprs: Array<Expression<Any>>,
    val functionReturnType: Class<*>,
)

object NonSuspendCallFunction {
    @Suppress("UNCHECKED_CAST")
    fun load(exprs: Array<out Expression<*>?>, targetExprIndex: Int, functionNameExprIndex: Int, parser: ParserInstance): CallFunction? {
        val targetExpr = LiteralUtils.defendExpression<Any>(exprs[targetExprIndex])

        val funcName = (exprs[functionNameExprIndex] as Expression<Identifier>).getSingle(null) ?: let {
            Skript.error("Function name is not specified. ${exprs[1]}")
            return null
        }

        val argumentsExpr: Expression<Any>? = exprs[2]?.let { LiteralUtils.defendExpression(it) }

        val argumentExprs =
            when (argumentsExpr) {
                is ExpressionList<*> -> (argumentsExpr as ExpressionList<Any>).expressions as Array<Expression<Any>>
                null -> arrayOf()
                else -> arrayOf(argumentsExpr)
            }

        argumentExprs.forEach {
            if (checkSingletonError(it)) return null
        }

        val argumentTypes = argumentExprs.map(Expression<Any>::getReturnType).toTypedArray()
        val calledClass = targetExpr.getReturnType()

        val methods = calledClass.methods.filter { it.name == internalFunctionNameOf(funcName.identifier) }

        if (methods.isEmpty()) {
            Skript.error("Cannot find function '$funcName' in '${calledClass.simpleName}'.")
            return null
        }

        val method = methods
            .filter { it.parameters.size - 1 == argumentTypes.size }
            .firstOrNull { method ->
                method.parameters
                    .drop(1)
                    .filterIndexed { index, parameter -> !parameter.type.isAssignableFrom(argumentTypes[index]) }
                    .isEmpty()
            }
            ?: let {
                Skript.error("Cannot find function '$funcName(${argumentTypes.joinToString(", ") { it.simpleName }})' in '${calledClass.simpleName}'.")
                return null
            }

        val modifierMetadata = method.getAnnotation(ModifierMetadata::class.java)

        if (calledClass.checkAccessError(parser, modifierMetadata.modifiers, "Cannot call function '$funcName' in '${calledClass.simpleName}' because it is private function.")) return null

        val methodCaller = methodCallers[method] ?: let {
            Skript.error("Internal error: MethodCaller for function '$funcName' in '${calledClass.simpleName}' is not found.")
            return null
        }

        val functionReturnType = method.returnType

        return CallFunction(targetExpr, methodCaller, argumentExprs, functionReturnType)
    }

    fun callFunction(
        e: Event,
        targetExpr: Expression<Any>,
        methodCaller: MethodCaller,
        argumentExprs: Array<Expression<Any>>,
        functionMediator: RuntimeFunctionMediator
    ) {
        val target = targetExpr.getSingle(e) ?: throw IllegalStateException("Target is null.")

        val args = if (argumentExprs.isEmpty()) {
            Array<Any>(0) { throw IndexOutOfBoundsException() }
        } else {
            Array(argumentExprs.size) { argumentExprs[it].getSingle(e) }
        }

        try {
            methodCaller.call(target, functionMediator, *args)
        } finally {
            AmbiguousVariableFrames.endFrame(functionMediator)
        }
    }
}
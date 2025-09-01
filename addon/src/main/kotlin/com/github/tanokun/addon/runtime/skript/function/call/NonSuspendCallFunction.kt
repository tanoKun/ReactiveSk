package com.github.tanokun.addon.runtime.skript.function.call

import ch.njol.skript.Skript
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionList
import ch.njol.skript.lang.parser.ParserInstance
import ch.njol.skript.util.LiteralUtils
import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.intermediate.generator.internalFunctionNameOf
import com.github.tanokun.addon.intermediate.metadata.MethodMetadata
import com.github.tanokun.addon.runtime.DynamicAccessChecks.checkAccessError
import com.github.tanokun.addon.runtime.MethodHandleInvokerUtil
import com.github.tanokun.addon.runtime.skript.SkriptExpressionInitChecks.checkSingletonError
import com.github.tanokun.addon.runtime.skript.function.call.mediator.RuntimeFunctionMediator
import com.github.tanokun.addon.runtime.variable.AmbiguousVariableFrames
import org.bukkit.event.Event
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

data class CallFunction(
    val targetExpr: Expression<Any>,
    val methodHandleCaller: (Array<Any?>, Any, Any) -> Unit,
    val methodHandle: MethodHandle,
    val argumentExprs: Array<Expression<Any>>,
    val functionReturnType: Class<*>,
)

object NonSuspendCallFunction {
    private val lookup = MethodHandles.publicLookup()

    @Suppress("UNCHECKED_CAST")
    fun load(exprs: Array<out Expression<*>?>, targetExprIndex: Int, functionNameExprIndex: Int, parser: ParserInstance): CallFunction? {
        val targetExpr = LiteralUtils.defendExpression<Any>(exprs[targetExprIndex])

        val funcName = (exprs[functionNameExprIndex] as Expression<Identifier>).getSingle(null) ?: let {
            Skript.error("Function name is not specified. ${exprs[1]}")
            return null
        }

        val argumentsExpr: Expression<Any> = LiteralUtils.defendExpression(exprs[2] ?: ExpressionList(arrayOf(), Any::class.java, false))

        val argumentExprs =
            if (argumentsExpr is ExpressionList<*>)
                (argumentsExpr as ExpressionList<Any>).expressions as Array<Expression<Any>>
            else arrayOf(argumentsExpr)

        argumentExprs.forEach {
            if (checkSingletonError(it)) return null
        }

        val argumentTypes = argumentExprs.map(Expression<Any>::getReturnType).toTypedArray()
        val calledClass = targetExpr.getReturnType()

        val method = calledClass.methods.firstOrNull { it.name == internalFunctionNameOf(funcName.identifier) } ?: let {
            Skript.error("Cannot find function '$funcName' in '${calledClass.simpleName}'.")
            return null
        }

        val methodMetadata = method.getAnnotation(MethodMetadata::class.java)

        if (methodMetadata.argumentTypes.size - 1 != argumentTypes.size) {
            Skript.error("mismatched argument size of function '$funcName'. expected ${methodMetadata.argumentTypes.size - 1}, but actual is ${argumentTypes.size}.")
            return null
        }

        methodMetadata.argumentTypes.drop(1).forEachIndexed { i, type ->
            if (type.java != argumentTypes[i]) {
                Skript.error("mismatched index '$i' of argument type of function '$funcName'. expected ${type.simpleName}, but actual is ${argumentTypes[i].simpleName}.")
                return null
            }
        }

        if (calledClass.checkAccessError(parser, methodMetadata.modifiers, "Cannot call function '$funcName' in '${calledClass.simpleName}' because it is private function.")) return null

        val (methodHandle, methodHandleCaller) = try {
            val methodHandle = lookup
                .unreflect(method)
                .asType(MethodType.methodType(Void.TYPE, Array(argumentTypes.size + 2) { Any::class.java }))

            methodHandle to MethodHandleInvokerUtil.buildFunction(argumentTypes.size, methodHandle)
        } catch (_: NoSuchMethodException) {
            Skript.error("Cannot find function '$funcName' in '${calledClass.simpleName}'.")
            return null
        }

        val functionReturnType = method.returnType

        return CallFunction(targetExpr, methodHandleCaller, methodHandle, argumentExprs, functionReturnType)
    }

    fun callFunction(e: Event, callFunction: CallFunction, functionMediator: RuntimeFunctionMediator) {
        val target = callFunction.targetExpr.getSingle(e) ?: throw IllegalStateException("Target is null.")

        val arguments = Array(callFunction.argumentExprs.size) { i -> callFunction.argumentExprs[i].getSingle(e) }

        try {
            callFunction.methodHandleCaller(arguments, target, functionMediator)
        } catch (e: Throwable) {
            e.printStackTrace()
        } finally {
            AmbiguousVariableFrames.endFrame(functionMediator)
        }
    }
}
package com.github.tanokun.addon.instance.call

import ch.njol.skript.lang.Expression
import com.github.tanokun.addon.clazz.ClassRegistry
import com.github.tanokun.addon.clazz.definition.Identifier
import com.github.tanokun.addon.clazz.definition.function.FunctionDefinition
import com.github.tanokun.addon.instance.AnyInstance
import com.github.tanokun.addon.instance.InstanceProperty
import org.bukkit.event.Event

inline fun call(
    funcNameExpr: Expression<Identifier>,
    targetExpr: Expression<Any>,
    argsExpr: Array<Expression<Any>>,
    event: Event,
    callback: (FunctionDefinition, AnyInstance, List<Pair<String, Any>>) -> Unit
): Boolean {
    val instanceCandidate = targetExpr.getSingle(event) ?: return false
    val funcName = funcNameExpr.getSingle(event) ?: return false

    val instance = getInstance(instanceCandidate) ?: return false

    val classDefinition = ClassRegistry.getClassDefinition(instance.className) ?: throw IllegalArgumentException("Class ${instance.className} is not defined.")
    val functionDefinition = classDefinition.getFunction(funcName) ?: throw IllegalArgumentException("Function $funcName is not defined in class ${instance.className}.")

    val arguments = functionDefinition.arguments(argsExpr, event)

    callback(functionDefinition, instance, arguments)

    return true
}

fun getInstance(candidate: Any) = when (candidate) {
    is AnyInstance -> candidate
    is InstanceProperty -> candidate.value as? AnyInstance
    else -> null
}
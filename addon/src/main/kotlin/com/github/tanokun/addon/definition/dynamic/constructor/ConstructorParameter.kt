package com.github.tanokun.addon.definition.dynamic.constructor

import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.definition.dynamic.DeclarationModifiers.isFactor
import com.github.tanokun.addon.definition.dynamic.DeclarationModifiers.isImmutable
import com.github.tanokun.addon.definition.dynamic.DeclarationModifiers.isMutable
import com.github.tanokun.addon.definition.dynamic.DeclarationModifiers.isProperty
import com.github.tanokun.addon.definition.dynamic.field.FieldDefinition

data class ConstructorParameter(
    val parameterName: Identifier,
    val typeName: Identifier,
    val isArray: Boolean,
    val declarationModifier: Int,
    val modifier: Int
) {
    fun toFieldDefinition(): FieldDefinition {
        if (!declarationModifier.isProperty()) throw IllegalArgumentException("Parameter is not property: $parameterName")

        return FieldDefinition(parameterName, typeName, declarationModifier, isArray, modifier)
    }

    fun isMutable(): Boolean = declarationModifier.isMutable()

    fun isImmutable(): Boolean = declarationModifier.isImmutable()

    fun isFactor(): Boolean = declarationModifier.isFactor()

    fun isProperty(): Boolean = declarationModifier.isProperty()
}
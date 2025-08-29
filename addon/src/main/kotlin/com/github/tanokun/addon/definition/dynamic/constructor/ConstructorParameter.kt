package com.github.tanokun.addon.definition.dynamic.constructor

import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.definition.dynamic.field.FieldDefinition

data class ConstructorParameter(
    val parameterName: Identifier,
    val typeName: Identifier,
    val isArray: Boolean,
    val isProperty: Boolean,
    val isMutable: Boolean,
    val modifier: Int
) {
    fun toFieldDefinition(): FieldDefinition {
        if (!isProperty) throw IllegalArgumentException("Parameter is not property: $parameterName")

        return FieldDefinition(parameterName, typeName, isMutable, isArray, modifier)
    }
}
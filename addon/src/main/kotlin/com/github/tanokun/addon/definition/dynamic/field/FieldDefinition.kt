package com.github.tanokun.addon.definition.dynamic.field

import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.definition.dynamic.PropertyModifier
import com.github.tanokun.addon.definition.dynamic.PropertyModifiers.isFactor

data class FieldDefinition(
    val fieldName: Identifier,
    val typeName: Identifier,
    val modifiers: PropertyModifier,
    val isArray: Boolean,
) {

    fun isFactor(): Boolean = modifiers.isFactor()
}
package com.github.tanokun.addon.definition.dynamic.field

import com.github.tanokun.addon.definition.Identifier


data class FieldDefinition(
    val fieldName: Identifier,
    val typeName: Identifier,
    val isMutable: Boolean,
    val isArray: Boolean,
    val modifier: Int
) {

    override fun toString(): String {
        val mutability = if (isMutable) "var" else "val"
        val typeStr = if (isArray) "array of $typeName" else typeName
        return "Field(${modifier} $mutability $fieldName: $typeStr)"
    }
}
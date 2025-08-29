package com.github.tanokun.addon.definition.variable

import com.github.tanokun.addon.definition.Identifier

data class TypedVariableDeclaration(
    val variableName: Identifier,
    val type: Class<*>,
    val isMutable: Boolean,
    val scopeCount: Int,
) {
    init {
        if (scopeCount < 0) throw IllegalArgumentException("Scope must be non-negative.")
    }
}
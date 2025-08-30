package com.github.tanokun.addon.definition.variable

import com.github.tanokun.addon.definition.Identifier

data class TypedVariableDeclaration(
    val variableName: Identifier,
    val type: Class<*>,
    val isMutable: Boolean,
    val depth: Int,       // 宣言されたスコープ深度（0 起点）
    val index: Int = -1,       // 解析時に採番される globalIndex（未採番は -1）
)

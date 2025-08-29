package com.github.tanokun.addon.definition.dynamic.function

import com.github.tanokun.addon.definition.Identifier

data class ParameterDefinition(
    val parameterName: Identifier,
    val typeName: Identifier,
    val isArray: Boolean
)
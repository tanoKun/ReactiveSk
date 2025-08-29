package com.github.tanokun.addon.definition.dynamic.function

import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.definition.dynamic.error.ThrowType

data class FunctionDefinition(
    val name: Identifier,
    val parameters: List<ParameterDefinition>,
    val returnTypeName: Identifier?,
    val modifier: Int,
    val throwsErrors: List<ThrowType>
)
package com.github.tanokun.addon.clazz.definition.parse.function

import com.github.tanokun.addon.clazz.definition.function.FunctionDefinition

sealed class ParseFunctionResult {
        data class Success(val definition: FunctionDefinition) : ParseFunctionResult()
        data class Failure(val errorMessage: String) : ParseFunctionResult()
    }
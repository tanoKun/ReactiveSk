package com.github.tanokun.addon.clazz.definition.parse.field

import com.github.tanokun.addon.clazz.definition.field.FieldDefinition

sealed class ParseFieldResult {
    class Success(val unidentifiedClassDefinition: FieldDefinition) : ParseFieldResult()
    data class Failure(val errorMessage: String, val line: String) : ParseFieldResult()
}
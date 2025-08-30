package com.github.tanokun.addon.definition.dynamic.field

import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.definition.dynamic.DeclarationModifiers.isFactor
import com.github.tanokun.addon.definition.dynamic.DeclarationModifiers.isImmutable
import com.github.tanokun.addon.definition.dynamic.DeclarationModifiers.isMutable
import com.github.tanokun.addon.definition.dynamic.DeclarationModifiers.isProperty


data class FieldDefinition(
    val fieldName: Identifier,
    val typeName: Identifier,
    val declarationModifier: Int,
    val isArray: Boolean,
    val modifier: Int
) {

    fun isMutable(): Boolean = declarationModifier.isMutable()

    fun isImmutable(): Boolean = declarationModifier.isImmutable()

    fun isFactor(): Boolean = declarationModifier.isFactor()

    fun isProperty(): Boolean = declarationModifier.isProperty()
}
package com.github.tanokun.addon.definition.dynamic

import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.definition.dynamic.constructor.ConstructorParameter
import com.github.tanokun.addon.definition.dynamic.constructor.InitSection
import com.github.tanokun.addon.definition.dynamic.field.FieldDefinition
import com.github.tanokun.addon.definition.dynamic.function.FunctionDefinition

data class ClassDefinition(
    val className: Identifier,
    val constructorParameters: List<ConstructorParameter>,
    val fields: List<FieldDefinition>,
    val functions: List<FunctionDefinition>,
    val initSection: InitSection
) {

    /**
     * 初期化が必要なフィールドのリストを取得します。
     * コンストラクタパラメータとして宣言されていないフィールドのみを返します。
     *
     * @return 初期化が必要なフィールドのリスト
     */
    fun getRequiredInitializationFields() = fields - constructorParameters.filter { it.isProperty() }.map { it.toFieldDefinition() }

    fun getAllUsedTypes(): Set<Identifier> {
        val types = mutableSetOf<Identifier>()

        constructorParameters.forEach { types.add(it.typeName) }

        fields.forEach { types.add(it.typeName) }

        functions.forEach { func ->
            func.returnTypeName?.let { types.add(it) }
            func.parameters.forEach { types.add(it.typeName) }
        }

        return types
    }
}
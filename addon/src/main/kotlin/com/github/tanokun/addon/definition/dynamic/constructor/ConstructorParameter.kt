package com.github.tanokun.addon.definition.dynamic.constructor

import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.definition.dynamic.PropertyModifier
import com.github.tanokun.addon.definition.dynamic.PropertyModifiers.isFactor
import com.github.tanokun.addon.definition.dynamic.PropertyModifiers.isProperty
import com.github.tanokun.addon.definition.dynamic.field.FieldDefinition

/**
 * コンストラクタのパラメータを表現するクラス。
 *
 * @property parameterName パラメータの名前
 * @property typeName パラメータの型名
 * @property isArray 配列型かどうか
 * @property modifiers アクセス修飾子
 * @property isFactor ファクターとして扱うかどうか
 * @property modifiers その他の修飾子
 *
 * public または private 修飾子が設定されている場合、
 * そのパラメータはクラスのプロパティとしても機能します。
 *
 * Modifier.TRANSIENT が Factor を代替します。
 */
data class ConstructorParameter(
    val parameterName: Identifier,
    val typeName: Identifier,
    val modifiers: PropertyModifier,
    val isArray: Boolean,
) {
    fun isProperty(): Boolean = modifiers.isProperty()

    fun toFieldDefinition(): FieldDefinition {
        if (!isProperty()) throw IllegalArgumentException("Parameter is not property: $parameterName")

        return FieldDefinition(parameterName, typeName, modifiers, isArray)
    }
}
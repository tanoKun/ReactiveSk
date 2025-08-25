package com.github.tanokun.addon.clazz.definition.function

import ch.njol.skript.classes.ClassInfo
import com.github.tanokun.addon.clazz.definition.Identifier

/**
 * 関数の引数など、名前と型を持つパラメータの定義を表すデータクラス。
 * @param name パラメータ名。
 * @param typeName 型名。
 * @param isArray `true`なら配列型。
 */
data class ParameterDefinition(
    val name: Identifier,
    val typeName: Identifier,
    val isArray: Boolean
)
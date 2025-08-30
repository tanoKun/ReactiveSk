package com.github.tanokun.addon.definition

/**
 * 関数名、変数名を表します。これは [ch.njol.skript.registrations.Classes] に登録されますが、
 * 基本的に競合するため、既存の登録されているクラスの [ch.njol.skript.classes.ClassInfo.codeName] と被っているものは
 * [com.github.tanokun.addon.definition.Identifier] として振る舞いません。
 *
 * 動的クラス名などを取得したい場合は [DynamicClassInfo] を使用してください。
 */
data class Identifier(val identifier: String) {
    override fun toString(): String = identifier

    companion object {
        val empty = Identifier("")
    }
}
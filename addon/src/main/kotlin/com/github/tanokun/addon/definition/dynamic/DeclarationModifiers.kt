package com.github.tanokun.addon.definition.dynamic

typealias DeclarationModifier = Int

object DeclarationModifiers {

    const val MUTABLE: Int = 1 shl 0
    const val IMMUTABLE: Int = 1 shl 1
    const val FACTOR: Int = 1 shl 2

    /**
     * 指定された整数が `mutable` または `factor` フラグを持つかを確認します。
     * `factor` は `mutable` としても振る舞います。
     *
     * @param modifiers 確認対象の整数フラグ
     * @return `mutable` または `factor` であれば `true`、そうでなければ `false`
     */
    fun DeclarationModifier.isMutable(): Boolean {
        return (this and (MUTABLE or FACTOR)) != 0
    }

    /**
     * 指定された整数が `immutable` フラグを持つかを確認します。
     *
     * @param modifiers 確認対象の整数フラグ
     * @return `immutable` であれば `true`、そうでなければ `false`
     */
    fun DeclarationModifier.isImmutable(): Boolean {
        return (this and IMMUTABLE) != 0
    }

    /**
     * 指定された整数が `factor` フラグを持つかを確認します。
     *
     * @param modifiers 確認対象の整数フラグ
     * @return `factor` であれば `true`、そうでなければ `false`
     */
    fun DeclarationModifier.isFactor(): Boolean {
        return (this and FACTOR) != 0
    }

    fun DeclarationModifier.isProperty(): Boolean {
        return this != 0
    }
}
package com.github.tanokun.reactivesk.lang


typealias PropertyModifier = Int

object PropertyModifiers {
    /** public修飾子 (可視性) */
    const val PUBLIC: PropertyModifier = 1 shl 0 // 1

    /** private修飾子 (可視性) */
    const val PRIVATE: PropertyModifier = 1 shl 1 // 2

    /** factor修飾子 (振る舞い) */
    const val FACTOR: PropertyModifier = 1 shl 2 // 4

    /** mutable修飾子 (変更可能性) */
    const val MUTABLE: PropertyModifier = 1 shl 3 // 8


    // --- 状態をチェックするための拡張関数 ---

    /**
     * この修飾子セットに `PUBLIC` フラグが含まれているかを確認します。
     * @return publicであれば `true`
     */
    fun PropertyModifier.isPublic(): Boolean = (this and PUBLIC) != 0

    /**
     * この修飾子セットに `PRIVATE` フラグが含まれているかを確認します。
     * @return privateであれば `true`
     */
    fun PropertyModifier.isPrivate(): Boolean = (this and PRIVATE) != 0

    /**
     * この修飾子セットに `FACTOR` フラグが含まれているかを確認します。
     * @return factorであれば `true`
     */
    fun PropertyModifier.isFactor(): Boolean = (this and FACTOR) != 0

    /**
     * この修飾子セットに `MUTABLE` フラグが含まれているかを確認します。
     * @return mutableであれば `true`
     */
    fun PropertyModifier.isMutable(): Boolean = (this and (MUTABLE or FACTOR)) != 0

    /**
     * この修飾子セットに `MUTABLE` フラグが含まれていないことを確認します (イミュータブル)。
     * @return mutableでなければ (immutableであれば) `true`
     */
    fun PropertyModifier.isImmutable(): Boolean = !this.isMutable()

    fun PropertyModifier.isProperty(): Boolean = this.isMutable() || this.isFactor() || this.isImmutable()
}
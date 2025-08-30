package com.github.tanokun.addon.definition.dynamic

typealias DeclarationModifier = Int

object DeclarationModifiers {

    const val MUTABLE: Int = 1 shl 0
    const val IMMUTABLE: Int = 1 shl 1
    const val FACTOR: Int = 1 shl 2

    fun DeclarationModifier.isMutable(): Boolean {
        return (this and (MUTABLE or FACTOR)) != 0
    }

    fun DeclarationModifier.isImmutable(): Boolean {
        return (this and IMMUTABLE) != 0
    }

    fun DeclarationModifier.isFactor(): Boolean {
        return (this and FACTOR) != 0
    }

    fun DeclarationModifier.isProperty(): Boolean {
        return this != 0
    }
}
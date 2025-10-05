package com.github.tanokun.reactivesk.v263.skript.util

import ch.njol.skript.Skript
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.SyntaxElementInfo

/**
 * Skript の構文要素登録を簡略化するユーティリティです。
 *
 * ジェネリックな [Effect] 型を指定して構文を登録できます。
 */
object PriorityRegistration {
    /**
     * 指定した [T] 型のエフェクトを最優先で登録します。
     *
     * @param T 登録するエフェクトの型
     * @param syntax 登録する構文パターン群
     */
    inline fun <reified T: Effect> register(vararg syntax: String) {
        val effects = (Skript.getEffects() as ArrayList)
        val statements = (Skript.getStatements() as ArrayList)

        val originClassPath = Thread.currentThread().stackTrace[2].className
        val info = SyntaxElementInfo(syntax, T::class.java, originClassPath)

        effects.add(0, info)
        statements.add(0, info)
    }
}
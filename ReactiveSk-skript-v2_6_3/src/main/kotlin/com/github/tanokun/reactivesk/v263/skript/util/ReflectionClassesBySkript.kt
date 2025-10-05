package com.github.tanokun.reactivesk.v263.skript.util

import ch.njol.skript.Skript
import ch.njol.skript.classes.ClassInfo
import ch.njol.skript.registrations.Classes
import java.util.regex.Pattern

/**
 * Skript のクラス登録情報を参照してユーザ入力から対応するクラスを解決します。
 *
 * 通常参照できない一時的なクラス情報も考慮してクラスを検索します。
 */
object ReflectionClassesBySkript {
    private val tempClassInfosField = Classes::class.java.getDeclaredField("tempClassInfos").apply {
        isAccessible = true
    }

    /**
     * ユーザ入力 [userInput] に対応するクラスを取得します。
     *
     * @param userInput ユーザが指定したクラス名またはパターン
     *
     * @return 解決されたクラスオブジェクト または null
     */
    @Suppress("UNCHECKED_CAST")
    fun getClassBySkript(userInput: String): Class<*>? {
        val userInput = userInput.lowercase()

        if (!Skript.isAcceptRegistrations()) {
            Classes.getClassInfoNoError(userInput)?.c?.let { return it }
            return Classes.getClassFromUserInput(userInput)
        }

        for (ci in tempClassInfosField.get(null) as List<ClassInfo<*>>) {
            val uip: Array<out Pattern?> = ci.userInputPatterns ?: continue

            uip.filterNotNull()
                .forEach {
                    if (it.matcher(userInput).matches()) return ci.c
                }
        }

        return Classes.getClassInfoNoError(userInput)?.c
    }
}
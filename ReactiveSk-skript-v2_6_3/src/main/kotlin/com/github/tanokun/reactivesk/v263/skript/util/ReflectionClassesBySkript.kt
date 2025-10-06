package com.github.tanokun.reactivesk.v263.skript.util

import ch.njol.skript.registrations.Classes

object ReflectionClassesBySkript {
    @Suppress("UNCHECKED_CAST")
    fun getClassBySkript(userInput: String): Class<*>? {
        val userInput = userInput.lowercase()

        return Classes.getClassInfoNoError(userInput)?.c
    }
}
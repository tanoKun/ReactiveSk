package com.github.tanokun.addon.runtime.skript

import ch.njol.skript.Skript
import ch.njol.skript.lang.Expression

object SkriptExpressionInitChecks {
    fun checkSingletonError(expr: Expression<*>): Boolean {
        if (expr.isSingle) {
            Skript.error("The expression $expr must not be single.")
            return true
        }

        return false
    }
}
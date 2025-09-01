package com.github.tanokun.addon.runtime.skript

import ch.njol.skript.Skript
import ch.njol.skript.lang.Expression

object SkriptExpressionInitChecks {
    fun checkSingletonError(expr: Expression<*>): Boolean {
        if (!expr.isSingle) {
            Skript.error("Only single expressions are allowed here, but you used a list expression $expr.")
            return true
        }

        return false
    }
}
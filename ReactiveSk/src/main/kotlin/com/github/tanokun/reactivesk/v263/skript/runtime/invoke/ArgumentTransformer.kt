package com.github.tanokun.reactivesk.v263.skript.runtime.invoke

import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionList
import ch.njol.skript.util.LiteralUtils
import com.github.tanokun.reactivesk.v263.skript.runtime.SkriptExpressionInitChecks.checkSingletonError

object ArgumentTransformer {
    @Suppress("UNCHECKED_CAST")
    fun transformArguments(argumentsExpr: Expression<*>?): Array<Expression<Any>>? {
        val defendedExpr: Expression<Any>? = argumentsExpr?.let { LiteralUtils.defendExpression(it) }

        val argumentExprs = when (defendedExpr) {
            is ExpressionList<Any> -> defendedExpr.expressions as Array<Expression<Any>>
            null -> arrayOf()
            else -> arrayOf(defendedExpr)
        }

        argumentExprs.forEach {
            if (checkSingletonError(it)) return null
        }

        return argumentExprs
    }
}

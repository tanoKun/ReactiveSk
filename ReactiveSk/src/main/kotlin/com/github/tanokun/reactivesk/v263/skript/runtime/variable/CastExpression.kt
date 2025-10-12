package com.github.tanokun.reactivesk.v263.skript.runtime.variable

import ch.njol.skript.Skript
import ch.njol.skript.classes.ClassInfo
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionType
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.skript.util.LiteralUtils
import ch.njol.util.Kleenean
import org.bukkit.event.Event

@Suppress("UNCHECKED_CAST")
class CastExpression: SimpleExpression<Any>() {
    companion object {
        fun register() {
            Skript.registerExpression(CastExpression::class.java, Any::class.java, ExpressionType.SIMPLE,
                "%object% as %classinfo%",
                "cast %object% to %classinfo%"
            )
        }
    }

    private lateinit var targetExpr: Expression<Any>

    private lateinit var classInfo: ClassInfo<*>

    override fun init(
        exprs: Array<out Expression<*>>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult,
    ): Boolean {
        classInfo = (exprs[1].getSingle(null) as? ClassInfo<*>) ?: let {
            Skript.error("Not found class info: $it.")
            return false
        }

        targetExpr = LiteralUtils.defendExpression(exprs[0])

        return true
    }

    override fun get(e: Event): Array<Any>? {
        val target = targetExpr.getSingle(e) ?: return null

        if (!classInfo.c.isAssignableFrom(target.javaClass)) {
            Skript.error("Cannot cast ${target.javaClass} to ${classInfo.c}.")
            return null
        }

        return arrayOf(target)
    }

    override fun isSingle(): Boolean = true

    override fun getReturnType(): Class<out Any?> = classInfo.c

    override fun toString(e: Event?, debug: Boolean): String = "${targetExpr.toString(e, debug)} as ${classInfo.name}"
}
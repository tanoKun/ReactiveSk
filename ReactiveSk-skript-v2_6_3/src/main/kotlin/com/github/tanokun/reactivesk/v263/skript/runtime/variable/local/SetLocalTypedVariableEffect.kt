package com.github.tanokun.reactivesk.v263.skript.runtime.variable.local

import ch.njol.skript.Skript
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.TriggerItem
import ch.njol.skript.util.LiteralUtils
import ch.njol.util.Kleenean
import com.github.tanokun.reactivesk.compiler.frontend.analyze.variable.TypedVariableDeclaration
import com.github.tanokun.reactivesk.v263.AmbiguousVariableFrames
import com.github.tanokun.reactivesk.v263.ReactiveSkAddon
import org.bukkit.event.Event

@Suppress("UNCHECKED_CAST")
class SetLocalTypedVariableEffect: Effect() {
    companion object {
        fun register() {
            Skript.registerEffect(SetLocalTypedVariableEffect::class.java,
                "set \\[%identifier%\\] to %object%",
            )
        }
    }

    private val typedVariableResolver = ReactiveSkAddon.typedVariableResolver

    private lateinit var definitionExpr: Expression<Any>

    private lateinit var declaration: TypedVariableDeclaration.Resolved

    override fun init(
        exprs: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult,
    ): Boolean {
        this.declaration = verifyAndTouchTypedVariable(exprs[0], parser, typedVariableResolver) ?: return false

        val type = declaration.type

        definitionExpr = (exprs.getOrNull(1) as? Expression<Any>)?.let { LiteralUtils.defendExpression(it) } ?: let {
            Skript.error("Definition expression ${exprs[1]} is not invalid.")
            return false
        }

        if (!definitionExpr.isSingle) {
            Skript.error("Definition '$definitionExpr' must be single.")
            return false
        }

        if (!type.isAssignableFrom(definitionExpr.returnType)) {
            Skript.error("Cannot assign $definitionExpr to ${exprs[0]?.getSingle(null)} because it's not type '${type.simpleName}' but '${definitionExpr.returnType.simpleName}'")
            return false
        }

        return true
    }

    override fun execute(e: Event) = throw UnsupportedOperationException("Cannot execute SetLocalTypedVariableEffect directly.")

    override fun walk(e: Event): TriggerItem? {
        run verify@ {
            if (declaration.isMutable) return@verify

            if (AmbiguousVariableFrames.get(e, declaration.index) != null) {
                Skript.error("Typed variable '${declaration.variableName}' is initialized.")
                return null
            }
        }

        val value = definitionExpr.getSingle(e)
        AmbiguousVariableFrames.set(e, declaration.index, value)

        return next
    }

    override fun toString(e: Event?, debug: Boolean): String = ""
}
package com.github.tanokun.reactivesk.v263.skript.runtime.variable.local

import ch.njol.skript.Skript
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionType
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.util.Kleenean
import ch.njol.util.coll.CollectionUtils.array
import com.github.tanokun.reactivesk.compiler.frontend.analyze.variable.TypedVariableDeclaration
import com.github.tanokun.reactivesk.v263.AmbiguousVariableFrames
import com.github.tanokun.reactivesk.v263.ReactiveSkAddon
import org.bukkit.event.Event


@Suppress("UNCHECKED_CAST")
class GetLocalTypedVariableExpression: SimpleExpression<Any>() {
    companion object {
        fun register() {
            Skript.registerExpression(
                GetLocalTypedVariableExpression::class.java, Any::class.java, ExpressionType.COMBINED,
                "\\[%identifier%\\]"
            )
        }
    }

    private val typedVariableResolver = ReactiveSkAddon.typedVariableResolver

    private lateinit var declaration: TypedVariableDeclaration.Resolved

    private var isArray: Boolean = false

    override fun init(
        exprs: Array<out Expression<*>>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult,
    ): Boolean {
        this.declaration = verifyAndTouchTypedVariable(exprs[0], parser, typedVariableResolver) ?: return false

        isArray = declaration.type == ArrayList::class.java

        return true
    }

    override fun get(e: Event): Array<Any> {
        val value = AmbiguousVariableFrames.get(e, declaration.index) ?: let {
            throw IllegalStateException("Typed variable '${declaration.variableName}' is not initialized.")
        }

        return array(value)
    }

    override fun getAll(e: Event) = get(e)

    override fun iterator(e: Event): Iterator<Any>? {
        if (isArray) {
            val list = getSingle(e) as ArrayList<*>
            return list.filterNotNull().iterator()
        }

        return null
    }

    override fun isSingle(): Boolean = true

    override fun getReturnType(): Class<out Any?> = declaration.type

    override fun toString(e: Event?, debug: Boolean): String = "${declaration.variableName} (${declaration.type.simpleName})"
}
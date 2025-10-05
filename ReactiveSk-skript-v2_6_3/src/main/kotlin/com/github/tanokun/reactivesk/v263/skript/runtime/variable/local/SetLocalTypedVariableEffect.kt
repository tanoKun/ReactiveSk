package com.github.tanokun.reactivesk.v263.skript.runtime.variable.local

import ch.njol.skript.Skript
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.util.LiteralUtils
import ch.njol.skript.variables.Variables
import ch.njol.util.Kleenean
import com.github.tanokun.reactivesk.compiler.frontend.analyze.variable.TypedVariableDeclaration
import com.github.tanokun.reactivesk.lang.Identifier
import com.github.tanokun.reactivesk.v263.AmbiguousVariableFrames
import com.github.tanokun.reactivesk.v263.ReactiveSkAddon
import com.github.tanokun.reactivesk.v263.skript.util.getDepth
import com.github.tanokun.reactivesk.v263.skript.util.getTopNode
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

    private lateinit var internalTypedVariable: String

    override fun init(
        exprs: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult,
    ): Boolean {
        val variableName = (exprs[0] as? Expression<Identifier>)?.getSingle(null) ?: let {
            Skript.error("Variable name ${exprs[0]} is not invalid.")
            return false
        }

        val node = parser.node ?: let {
            Skript.error("Cannot find node.")
            return false
        }

        val depth = node.getDepth()
        val topNode = node.getTopNode()

        val currentSection = parser.currentSections.lastOrNull()

        typedVariableResolver.touchSection(topNode, depth, currentSection)

        this.declaration = typedVariableResolver.getDeclarationInScopeChain(topNode, depth, variableName) ?: let {
            Skript.error("Typed variable '$variableName' is not declared in scope chain.")
            return false
        }

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
            Skript.error("Cannot assign $definitionExpr to '$variableName' because it's not type '${type.simpleName}' but '${definitionExpr.returnType.simpleName}'")
            return false
        }

        return true
    }

    override fun execute(e: Event) {
        val value = definitionExpr.getSingle(e)

        if (declaration.isMutable) {
            AmbiguousVariableFrames.set(e, declaration.index, value)
            return
        }

        if (Variables.getVariable(internalTypedVariable, e, true) != null) {
            val onDebug = if (Skript.debug()) "debug -> [internal: $internalTypedVariable, declaration: $declaration]" else ""
            throw IllegalStateException("Typed variable '${declaration.variableName}' is initialized." + onDebug)
        }

        AmbiguousVariableFrames.set(e, declaration.index, value)
    }

    override fun toString(e: Event?, debug: Boolean): String = ""
}
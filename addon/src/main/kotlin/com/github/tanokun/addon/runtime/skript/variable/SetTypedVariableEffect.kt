package com.github.tanokun.addon.runtime.skript.variable

import ch.njol.skript.Skript
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.sections.SecLoop
import ch.njol.skript.sections.SecWhile
import ch.njol.skript.util.LiteralUtils
import ch.njol.skript.variables.Variables
import ch.njol.util.Kleenean
import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.definition.variable.TypedVariableDeclaration
import com.github.tanokun.addon.definition.variable.TypedVariableResolver
import com.github.tanokun.addon.definition.skript.variable.getScopeCount
import com.github.tanokun.addon.definition.skript.variable.getTopNode
import org.bukkit.event.Event

@Suppress("UNCHECKED_CAST")
class SetTypedVariableEffect: Effect() {
    companion object {
        init {
            Skript.registerEffect(SetTypedVariableEffect::class.java,
                "\\[%identifier%\\] (\\:= | ->) %object%",
            )
        }
    }

    private lateinit var definitionExpr: Expression<Any>

    private lateinit var declaration: TypedVariableDeclaration

    private lateinit var internalTypedVariable: String

    override fun init(
        exprs: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult,
    ): Boolean {
        val variableName = (exprs[0] as? Expression<Identifier>)?.getSingle(null) ?: let {
            Skript.error("Variable name '$exprs[0]' is not invalid.")
            return false
        }

        val node = parser.node ?: let {
            Skript.error("Cannot find node.")
            return false
        }

        val scopeCount = node.getScopeCount()
        val topNode = node.getTopNode()

        val declaration = TypedVariableResolver.getDeclarationInScopeChain(topNode, scopeCount, variableName)

        if (declaration == null) {
            Skript.error("Typed variable '$variableName' is not declared in scope chain.")
            return false
        }

        this.declaration = declaration
        val type = declaration.type

        definitionExpr = (exprs.getOrNull(1) as? Expression<Any>)?.let { LiteralUtils.defendExpression(it) } ?: let {
            Skript.error("Definition expression '${exprs[1]}' is not invalid.")
            return false
        }

        if (!definitionExpr.isSingle) {
            Skript.error("Definition '$definitionExpr' must be single.")
            return false
        }

        if (!type.isAssignableFrom(definitionExpr.returnType)) {
            Skript.error("Cannot assign '$definitionExpr' to '$variableName' because it's not type '${type.simpleName}' but '${definitionExpr.returnType.simpleName}'")
            return false
        }

        internalTypedVariable = internalTypedVariableOf(variableName, declaration.scopeCount)

        return true
    }

    override fun execute(e: Event) {
        val value = definitionExpr.getSingle(e)

        if (declaration.isMutable) {
            Variables.setVariable(internalTypedVariable, value, e, true)
            return
        }

        if (Variables.getVariable(internalTypedVariable, e, true) != null) {
            val onDebug = if (Skript.debug()) "debug -> [internal: $internalTypedVariable, declaration: $declaration]" else ""
            throw IllegalStateException("Typed variable '${declaration.variableName}' is initialized." + onDebug)
        }

        Variables.setVariable(internalTypedVariable, value, e, true)
    }

    override fun toString(e: Event?, debug: Boolean): String = ""
}
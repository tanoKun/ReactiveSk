package com.github.tanokun.addon.definition.skript.variable

import ch.njol.skript.Skript
import ch.njol.skript.config.Node
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.parser.ParserInstance
import ch.njol.skript.registrations.Classes
import ch.njol.skript.sections.SecLoop
import ch.njol.skript.sections.SecWhile
import ch.njol.skript.util.LiteralUtils
import ch.njol.skript.variables.Variables
import ch.njol.util.Kleenean
import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.definition.variable.TypedVariableDeclaration
import com.github.tanokun.addon.definition.variable.TypedVariableResolver
import com.github.tanokun.addon.runtime.skript.variable.internalTypedVariableOf
import org.bukkit.event.Event

@Suppress("UNCHECKED_CAST")
class TypedVariableDeclarationEffect: Effect() {
    companion object {
        init {
            Skript.registerEffect(TypedVariableDeclarationEffect::class.java,
                "val %identifier% [\\(%identifier%\\)] \\:= %object%",
                "var %identifier% [\\(%identifier%\\)] \\:= %object%",
                "val %identifier% \\(%identifier%\\)",
                "var %identifier% \\(%identifier%\\)",
            )
        }
    }

    private var definitionExpr: Expression<Any>? = null

    private lateinit var declaration: TypedVariableDeclaration

    private lateinit var internalTypedVariable: String

    override fun init(
        exprs: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult,
    ): Boolean {
        val isMutable = matchedPattern == 1 || matchedPattern == 3

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

        if (isDuplicatingVariableDeclaration(topNode, scopeCount, variableName)) {
            Skript.error("Typed variable '$variableName' is already declared in this scope.")
            return false
        }

        var resolveType: Class<*> = Any::class.java
        val unresolveType = (exprs[1] as? Expression<Identifier>)?.getSingle(null)
        if (unresolveType != Identifier.empty)
            resolveType = unresolveType?.identifier?.lowercase()?.let(Classes::getClassInfoNoError)?.c ?: let {
                Skript.error("Type '$unresolveType' is not exist.")
                return false
            }

        val definitionExpr = (exprs.getOrNull(2) as? Expression<Any>)?.let { LiteralUtils.defendExpression<Any>(it) }

        shouldExecuteTypeInference(unresolveType, definitionExpr) { definitionExpr ->
            resolveType = definitionExpr.returnType
        }

        shouldCheckDefinitionType(unresolveType, definitionExpr) { definitionExpr ->
            if (!resolveType.isAssignableFrom(definitionExpr.returnType)) {
                Skript.error("Cannot assign '$definitionExpr' to '$variableName' because it's not type '${resolveType.simpleName}' but '${definitionExpr.returnType.simpleName}'")
                return false
            }
        }


        if (definitionExpr?.isSingle == false) {
            Skript.error("Definition expression '${exprs[1]}' must be single.")
            return false
        }

        if (shouldWarnAboutError(parser, definitionExpr)) {
            Skript.warning("Resolving variable '$variableName' inside a loop may cause 'already initialized' error if the variable is already initialized.")
        }

        definitionExpr?.let { this.definitionExpr = it }
        declaration = TypedVariableDeclaration(variableName, resolveType, isMutable, scopeCount)
        internalTypedVariable = internalTypedVariableOf(variableName, scopeCount)
        TypedVariableResolver.addDeclaration(topNode, declaration)

        return true
    }

    private inline fun shouldExecuteTypeInference(unresolveType: Identifier, definitionExpr: Expression<Any>?, dsl: (Expression<Any>) -> Unit) {
        if (unresolveType == Identifier.empty && definitionExpr != null) dsl(definitionExpr)
    }

    private inline fun shouldCheckDefinitionType(unresolveType: Identifier, definitionExpr: Expression<Any>?, dsl: (Expression<Any>) -> Unit) {
        if (unresolveType != Identifier.empty && definitionExpr != null) dsl(definitionExpr)
    }

    private fun isDuplicatingVariableDeclaration(topNode: Node, scopeCount: Int, variableName: Identifier): Boolean =
        TypedVariableResolver.getDeclarationInSingleScope(topNode, scopeCount, variableName) != null

    private fun shouldWarnAboutError(parser: ParserInstance, definitionExpr: Expression<*>?) =
        parser.currentSections.any { it is SecLoop || it is SecWhile } && definitionExpr == null

    override fun execute(e: Event) {
        definitionExpr?.let {
            val value = it.getSingle(e)

            if (value == null)
                throw NullPointerException("Null value '$definitionExpr' is not allowed for variable '${declaration.variableName}'.")

            Variables.setVariable(internalTypedVariable, value, e, true)
        }
    }

    override fun toString(e: Event?, debug: Boolean): String = "declare typed variable '${declaration.variableName}' (${declaration.type.simpleName})" + if (Skript.debug()) "debug -> [internal: $internalTypedVariable, declaration: $declaration]" else ""
}
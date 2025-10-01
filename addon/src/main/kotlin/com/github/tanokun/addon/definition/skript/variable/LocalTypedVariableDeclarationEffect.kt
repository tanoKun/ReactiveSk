package com.github.tanokun.addon.definition.skript.variable

import ch.njol.skript.Skript
import ch.njol.skript.classes.ClassInfo
import ch.njol.skript.config.Node
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.parser.ParserInstance
import ch.njol.skript.sections.SecLoop
import ch.njol.skript.sections.SecWhile
import ch.njol.skript.util.LiteralUtils
import ch.njol.util.Kleenean
import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.definition.variable.TypedVariableDeclaration
import com.github.tanokun.addon.definition.variable.TypedVariableResolver
import com.github.tanokun.addon.definition.variable.getDepth
import com.github.tanokun.addon.definition.variable.getTopNode
import com.github.tanokun.addon.runtime.variable.AmbiguousVariableFrames
import org.bukkit.event.Event

@Suppress("UNCHECKED_CAST")
class LocalTypedVariableDeclarationEffect: Effect() {
    companion object {
        init {
            Skript.registerEffect(LocalTypedVariableDeclarationEffect::class.java,
                "val %identifier% [\\(%-classinfo%\\)] \\:= %object%",
                "var %identifier% [\\(%-classinfo%\\)] \\:= %object%",
                "val %identifier% \\(%-classinfo%\\)",
                "var %identifier% \\(%-classinfo%\\)",
            )
        }
    }

    private var definitionExpr: Expression<Any>? = null

    private lateinit var declaration: TypedVariableDeclaration

    private var isFirstDeclaration = false

    private lateinit var topNode: Node

    private var variableCapacity: Int = -1

    override fun init(
        exprs: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult,
    ): Boolean {
        val isMutable = matchedPattern == 1 || matchedPattern == 3

        val variableName = (exprs[0] as? Expression<Identifier>)?.getSingle(null) ?: let {
            Skript.error("Variable name '${exprs[0]} is not invalid.")
            return false
        }

        val node = parser.node ?: let {
            Skript.error("Cannot find node.")
            return false
        }

        topNode = node.getTopNode()
        val depth = node.getDepth()

        val currentSection = parser.currentSections.lastOrNull()

        TypedVariableResolver.touchSection(topNode, depth, currentSection)

        if (isDuplicatingVariableDeclaration(topNode, depth, variableName)) {
            Skript.error("Typed variable '$variableName' is already declared in this scope.")
            return false
        }

        var resolveType: Class<*> = Any::class.java
        val unresolveType: ClassInfo<*>? =
            if (exprs[1] == null) null
            else (exprs[1] as Expression<ClassInfo<*>>).getSingle(null) ?: let {
                Skript.error("Type ${exprs[1]} is not exist.")
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
            Skript.error("Definition expression ${exprs[1]} must be single.")
            return false
        }

        if (shouldWarnAboutError(parser, definitionExpr)) {
            Skript.warning("Resolving variable '$variableName' inside a loop may cause 'already initialized' error if the variable is already initialized.")
        }

        definitionExpr?.let { this.definitionExpr = it }
        declaration = TypedVariableResolver.declare(topNode, TypedVariableDeclaration(variableName, resolveType, isMutable, depth))
        isFirstDeclaration = TypedVariableResolver.getIndexInNode(topNode) == 0

        return true
    }

    private inline fun shouldExecuteTypeInference(unresolveType: ClassInfo<*>?, definitionExpr: Expression<Any>?, dsl: (Expression<Any>) -> Unit) {
        if (unresolveType == null && definitionExpr != null) dsl(definitionExpr)
    }

    private inline fun shouldCheckDefinitionType(unresolveType: ClassInfo<*>?, definitionExpr: Expression<Any>?, dsl: (Expression<Any>) -> Unit) {
        if (unresolveType != null && definitionExpr != null) dsl(definitionExpr)
    }

    private fun isDuplicatingVariableDeclaration(topNode: Node, depth: Int, variableName: Identifier): Boolean =
        TypedVariableResolver.getDeclarationInSingleScope(topNode, depth, variableName) != null

    private fun shouldWarnAboutError(parser: ParserInstance, definitionExpr: Expression<*>?) =
        parser.currentSections.any { it is SecLoop || it is SecWhile } && definitionExpr == null

    override fun execute(e: Event) {
        if (isFirstDeclaration) {
            AmbiguousVariableFrames.beginFrame(e)
        }

        definitionExpr?.let {
            val value = it.getSingle(e)
                ?: throw NullPointerException("Null value '$definitionExpr' is not allowed for variable '${declaration.variableName}'.")

            AmbiguousVariableFrames.set(e, declaration.index, value)
        }
    }

    override fun toString(e: Event?, debug: Boolean): String = "declare typed variable '${declaration.variableName}' (${declaration.type.simpleName})"
}
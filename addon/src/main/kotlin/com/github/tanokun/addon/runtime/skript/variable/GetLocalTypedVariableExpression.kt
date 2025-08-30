package com.github.tanokun.addon.runtime.skript.variable

import ch.njol.skript.Skript
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionType
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.util.Kleenean
import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.definition.variable.TypedVariableDeclaration
import com.github.tanokun.addon.definition.variable.TypedVariableResolver
import com.github.tanokun.addon.definition.variable.getDepth
import com.github.tanokun.addon.definition.variable.getTopNode
import com.github.tanokun.addon.runtime.variable.VariableFrames
import org.bukkit.event.Event


@Suppress("UNCHECKED_CAST")
class GetLocalTypedVariableExpression: SimpleExpression<Any>() {
    companion object {
        init {
            Skript.registerExpression(
                GetLocalTypedVariableExpression::class.java, Any::class.java, ExpressionType.COMBINED,
                "\\[%*identifier%\\]"
            )
        }
    }

    private lateinit var declaration: TypedVariableDeclaration

    override fun init(
        exprs: Array<out Expression<*>>,
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

        TypedVariableResolver.touchSection(topNode, depth, parser.currentSections.firstOrNull())

        declaration = TypedVariableResolver.getDeclarationInScopeChain(topNode, depth, variableName) ?: let {
            Skript.error("Typed variable '$variableName' is not declared in scope chain.")
            return false
        }

        return true
    }

    override fun get(e: Event): Array<out Any> {
        val value = VariableFrames.get(e, declaration.index) ?: let {
            throw IllegalStateException("Typed variable '${declaration.variableName}' is not initialized.")
        }

        return arrayOf(value)

    }

    override fun toString(e: Event?, debug: Boolean): String = "[${declaration.variableName} (${declaration.type.simpleName})]"

    override fun isSingle(): Boolean = true

    override fun getReturnType() = declaration.type
}
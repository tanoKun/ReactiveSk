package com.github.tanokun.addon.runtime.skript.variable

import ch.njol.skript.Skript
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionType
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.skript.variables.Variables
import ch.njol.util.Kleenean
import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.definition.variable.TypedVariableDeclaration
import com.github.tanokun.addon.definition.variable.TypedVariableResolver
import com.github.tanokun.addon.definition.skript.variable.getScopeCount
import com.github.tanokun.addon.definition.skript.variable.getTopNode
import org.bukkit.event.Event
import kotlin.jvm.java

private const val INTERNAL_TYPED_VARIABLE_PREFIX = $$"_rSk$t$v$"

fun internalTypedVariableOf(name: Identifier, scopeCount: Int): String = "$INTERNAL_TYPED_VARIABLE_PREFIX$scopeCount$$name"

@Suppress("UNCHECKED_CAST")
class GetTypedVariableExpression: SimpleExpression<Any>() {
    companion object {
        init {
            Skript.registerExpression(
                GetTypedVariableExpression::class.java, Any::class.java, ExpressionType.COMBINED,
                "\\[%identifier%\\]"
            )
        }
    }

    private lateinit var internalTypedVariable: String

    private lateinit var declaration: TypedVariableDeclaration

    override fun init(
        exprs: Array<out Expression<*>>,
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
        internalTypedVariable = internalTypedVariableOf(variableName, scopeCount)
        declaration = TypedVariableResolver.getDeclarationInScopeChain(topNode, scopeCount, variableName) ?: let {
            Skript.error("Typed variable '$variableName' is not declared in scope chain.")
            return false
        }

        internalTypedVariable = internalTypedVariableOf(variableName, declaration.scopeCount)

        return true
    }

    override fun get(e: Event): Array<out Any> {
        val value = Variables.getVariable(internalTypedVariable, e, true) ?: let {
            val onDebug = if (Skript.debug()) "debug -> [internal: $internalTypedVariable, declaration: $declaration]" else ""
            throw IllegalStateException("Typed variable '${declaration.variableName}' is not initialized." + onDebug)
        }

        return arrayOf(value)

    }

    override fun toString(e: Event?, debug: Boolean): String = "[${declaration.variableName} (${declaration.type.simpleName})]"

    override fun isSingle(): Boolean = true

    override fun getReturnType() = declaration.type
}
package com.github.tanokun.addon.runtime.skript.variable

import ch.njol.skript.Skript
import ch.njol.skript.classes.Changer
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionType
import ch.njol.skript.lang.SkriptParser
import ch.njol.util.Checker
import ch.njol.util.Kleenean
import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.definition.variable.TypedVariableDeclaration
import com.github.tanokun.addon.definition.variable.TypedVariableResolver
import com.github.tanokun.addon.definition.variable.getDepth
import com.github.tanokun.addon.definition.variable.getTopNode
import com.github.tanokun.addon.runtime.variable.AmbiguousVariableFrames
import org.bukkit.event.Event


@Suppress("UNCHECKED_CAST")
class GetLocalTypedVariableExpression: Expression<Any> {
    companion object {
        init {
            Skript.registerExpression(
                GetLocalTypedVariableExpression::class.java, Any::class.java, ExpressionType.COMBINED,
                "\\[%*identifier%\\]"
            )
        }
    }

    private lateinit var declaration: TypedVariableDeclaration

    private var isArray: Boolean = false

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

        TypedVariableResolver.touchSection(topNode, depth, parser.currentSections.lastOrNull())

        declaration = TypedVariableResolver.getDeclarationInScopeChain(topNode, depth, variableName) ?: let {
            Skript.error("Typed variable '$variableName' is not declared in scope chain.")
            return false
        }

        isArray = declaration.type == ArrayList::class.java

        return true
    }

    override fun toString(e: Event?, debug: Boolean): String = "[${declaration.variableName} (${declaration.type.simpleName})]"

    override fun isSingle(): Boolean = true

    override fun getReturnType() = declaration.type

    override fun getAnd(): Boolean = true

    override fun setTime(time: Int): Boolean { return false }

    override fun getTime(): Int = 0

    override fun isDefault(): Boolean = false

    override fun iterator(e: Event): Iterator<Any>? {
        if (isArray) {
            val list = getSingle(e) as ArrayList<*>
            return list.filterNotNull().iterator()
        }

        return null
    }

    override fun isLoopOf(s: String): Boolean = false

    override fun getSource(): Expression<*> = this

    override fun simplify(): Expression<out Any> = this

    override fun acceptChange(mode: Changer.ChangeMode?): Array<out Class<*>> = arrayOf()

    override fun change(e: Event, delta: Array<out Any?>, mode: Changer.ChangeMode) { }

    override fun getSingle(e: Event): Any = AmbiguousVariableFrames.get(e, declaration.index) ?: let {
        throw IllegalStateException("Typed variable '${declaration.variableName}' is not initialized.")
    }

    override fun getArray(e: Event): Array<out Any> = arrayOf(getSingle(e))

    override fun getAll(e: Event): Array<out Any> = arrayOf(getSingle(e))

    override fun check(e: Event, c: Checker<in Any>, negated: Boolean): Boolean = true

    override fun check(e: Event, c: Checker<in Any>?): Boolean = true

    override fun <R : Any?> getConvertedExpression(vararg to: Class<R>): Expression<out R>? = null
}
package com.github.tanokun.addon.instance.call

import ch.njol.skript.Skript
import ch.njol.skript.classes.Changer
import ch.njol.skript.lang.*
import ch.njol.skript.lang.TriggerItem.walk
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.skript.timings.SkriptTimings
import ch.njol.skript.util.LiteralUtils
import ch.njol.skript.variables.Variables
import ch.njol.util.Kleenean
import com.github.tanokun.addon.clazz.ClassRegistry
import com.github.tanokun.addon.clazz.definition.Identifier
import com.github.tanokun.addon.coroutineScope
import com.github.tanokun.addon.instance.AnyInstance
import com.github.tanokun.addon.instance.call.call
import com.github.tanokun.addon.maker.function.AwaitFunctionRuntimeBukkitEvent
import com.github.tanokun.addon.maker.function.NonSuspendFunctionRuntimeBukkitEvent
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bukkit.event.Event

@Suppress("UNCHECKED_CAST")
class NonSuspendCallFunctionEffect : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                NonSuspendCallFunctionEffect::class.java,
                "%object%.%identifier%\\([%-objects%]\\)"
            )
        }
    }

    private lateinit var funcNameExpr: Expression<Identifier>
    private lateinit var targetExpr: Expression<Any>
    private lateinit var argsExpr: Array<Expression<Any>>
    private var variableExpr: Expression<Variable<*>>? = null

    override fun init(
        exprs: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult,
    ): Boolean {
        targetExpr = exprs[0] as Expression<Any>
        funcNameExpr = exprs[1] as Expression<Identifier>
        variableExpr = exprs.getOrNull(2) as? Expression<Variable<*>>

        val argsExpr: Expression<Any> = LiteralUtils.defendExpression(exprs[2] ?: ExpressionList(arrayOf(), Any::class.java, false))
        this.argsExpr =
            if (argsExpr is ExpressionList<*>)
                (argsExpr as ExpressionList<Any>).expressions as Array<Expression<Any>>
            else arrayOf(argsExpr)

        return true
    }

    override fun execute(e: Event) {
        call(funcNameExpr, targetExpr, argsExpr, e) { function, target, arguments ->
            val nonSuspendFunction = NonSuspendFunctionRuntimeBukkitEvent()
            runBlocking { function.call(target, arguments, nonSuspendFunction) }
        }
    }

    override fun toString(e: Event?, debug: Boolean): String? = "await call"
}

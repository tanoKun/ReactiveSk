package com.github.tanokun.addon.instance.observe.registry

import ch.njol.skript.Skript
import ch.njol.skript.classes.Changer
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionList
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.TriggerItem
import ch.njol.skript.lang.Variable
import ch.njol.skript.timings.SkriptTimings
import ch.njol.skript.util.LiteralUtils
import ch.njol.skript.variables.Variables
import ch.njol.util.Kleenean
import com.github.tanokun.addon.clazz.definition.Identifier
import com.github.tanokun.addon.coroutineScope
import com.github.tanokun.addon.instance.call.AwaitCallFunctionEffect
import com.github.tanokun.addon.instance.call.call
import com.github.tanokun.addon.maker.function.AwaitFunctionRuntimeBukkitEvent
import kotlinx.coroutines.launch
import org.bukkit.event.Event

@Suppress("UNCHECKED_CAST")
class SetInstanceValueNotifierEffect : Effect() {

    companion object {
        init {
/*            Skript.registerEffect(
                AwaitCallFunctionEffect::class.java,
                "await %object%.%identifier%\\([%-objects%]\\) on %~objects%"
            )*/
        }
    }

    private lateinit var funcNameExpr: Expression<Identifier>
    private lateinit var targetExpr: Expression<Any>
    private lateinit var argsExpr: Array<Expression<Any>>
    private lateinit var variableExpr: Expression<Variable<*>>

    override fun init(
        exprs: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult,
    ): Boolean {
        targetExpr = exprs[0] as Expression<Any>
        funcNameExpr = exprs[1] as Expression<Identifier>
        variableExpr = exprs[3] as Expression<Variable<*>>

        val argsExpr: Expression<Any> = LiteralUtils.defendExpression(exprs[2] ?: ExpressionList(arrayOf(), Any::class.java, false))
        this.argsExpr =
            if (argsExpr is ExpressionList<*>)
                (argsExpr as ExpressionList<Any>).expressions as Array<Expression<Any>>
            else arrayOf(argsExpr)

        return true
    }

    override fun walk(e: Event): TriggerItem? {
        call(funcNameExpr, targetExpr, argsExpr, e) { function, target, arguments ->
            val awaitFunction = AwaitFunctionRuntimeBukkitEvent()
            val arguments = function.arguments(argsExpr, e)
            val localVars = Variables.removeLocals(e)

            coroutineScope.launch {
                val returnValue = function.call(target, arguments, awaitFunction)
                val value = returnValue as? Array<*> ?: arrayOf(returnValue)

                if (localVars != null) Variables.setLocalVariables(e, localVars)
                variableExpr.change(e, value, Changer.ChangeMode.SET)

                var timing: Any? = null
                if (SkriptTimings.enabled()) {
                    val trigger = getTrigger()
                    if (trigger != null) {
                        timing = SkriptTimings.start(trigger.debugLabel)
                    }
                }

                walk(next, e)
                Variables.removeLocals(e)

                SkriptTimings.stop(timing)
            }

            return null
        }

        return next
    }
    override fun execute(e: Event) { throw UnsupportedOperationException() }

    override fun toString(e: Event?, debug: Boolean): String? = "await call"
}

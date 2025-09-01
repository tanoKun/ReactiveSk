package com.github.tanokun.addon.runtime.skript.function.call

import ch.njol.skript.Skript
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.util.Kleenean
import com.github.tanokun.addon.runtime.skript.function.call.NonSuspendCallFunction.callFunction
import com.github.tanokun.addon.runtime.skript.function.call.mediator.NonSuspendRuntimeFunctionMediator
import org.bukkit.event.Event

@Suppress("UNCHECKED_CAST")
class NonSuspendCallFunctionEffect : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                NonSuspendCallFunctionEffect::class.java,
                "call %*identifier% in %object% [with %-objects%]"
            )
        }
    }

    private lateinit var callFunction: CallFunction

    override fun init(
        exprs: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult,
    ): Boolean {
        val targetExprIndex = 1 //if (matchedPattern == 0) 0 else 1
        val functionNameExprIndex = 0 //if (matchedPattern == 0) 1 else 0

        callFunction = NonSuspendCallFunction.load(exprs, targetExprIndex, functionNameExprIndex, parser) ?: return false

        return true
    }

    override fun execute(e: Event) {
        callFunction(e, callFunction, NonSuspendRuntimeFunctionMediator())
    }

    override fun toString(e: Event?, debug: Boolean): String = "call function with ${callFunction.argumentExprs.map { it.toString(e, debug) }.reduceOrNull { acc, s -> "$acc, $s" }} in ${callFunction.targetExpr.toString(e, debug)}"
}

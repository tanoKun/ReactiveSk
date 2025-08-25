package com.github.tanokun.addon.instance.call

import ch.njol.skript.Skript
import ch.njol.skript.classes.ClassInfo
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.TriggerItem
import ch.njol.skript.sections.SecLoop
import ch.njol.skript.sections.SecWhile
import ch.njol.util.Kleenean
import com.github.tanokun.addon.maker.function.AwaitFunctionRuntimeBukkitEvent
import com.github.tanokun.addon.maker.ClassDefinitionBukkitEvent
import com.github.tanokun.addon.maker.function.FunctionRuntimeBukkitEvent
import com.github.tanokun.addon.maker.function.NonSuspendFunctionRuntimeBukkitEvent
import org.bukkit.event.Event

class FunctionReturnEffect : Effect() {

    companion object {
        init {
            Skript.registerEffect(FunctionReturnEffect::class.java, "fun return %objects%")
        }

        var currentlyReturn: ClassInfo<*>? = null
    }

    private lateinit var valueExpr: Expression<Any>

    override fun walk(e: Event): TriggerItem? {
        if (e !is FunctionRuntimeBukkitEvent) throw IllegalArgumentException("関数セクションではありません。")

        val value = if (valueExpr.isSingle) valueExpr.getSingle(e) else valueExpr.getArray(e)
        e.setReturn(value)

        var parent = getParent()

        while (parent != null) {
            if (parent is SecLoop) parent.exit(e)
            else if (parent is SecWhile) parent.reset()

            parent = parent.getParent()
        }

        return null
    }

    override fun toString(e: Event?, debug: Boolean): String = "return ${valueExpr.toString(e, debug)}"

    override fun init(
        exprs: Array<out Expression<*>>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult,
    ): Boolean {
        require(parser.currentEvents.any { it == ClassDefinitionBukkitEvent::class.java }) {
            Skript.error("'return' は関数の外では使用できません。")
            return false
        }

        val currentlyReturn = currentlyReturn ?: run {
            Skript.error("戻り値が設定されていない関数です。")
            return false
        }

        valueExpr = exprs[0].getConvertedExpression(currentlyReturn.c) as Expression<Any>
        return true
    }

    override fun execute(e: Event?) { }
}
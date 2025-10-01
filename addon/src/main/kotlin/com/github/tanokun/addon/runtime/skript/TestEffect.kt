package com.github.tanokun.addon.runtime.skript

import ch.njol.skript.Skript
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.SyntaxElementInfo
import ch.njol.util.Kleenean
import org.bukkit.event.Event

@Suppress("UNCHECKED_CAST")
class TestEffect: Effect() {
    companion object {
        init {
            val effects = (Skript.getEffects() as ArrayList)
            val statements = (Skript.getStatements() as ArrayList)

            val originClassPath = Thread.currentThread().stackTrace[2].className
            val info = SyntaxElementInfo(arrayOf("(wait|halt) [for] %timespan%"), TestEffect::class.java, originClassPath)

            effects.add(0, info)
            statements.add(0, info)
        }
    }

    override fun init(
        exprs: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult,
    ): Boolean {


        return false
    }

    override fun execute(e: Event) {
    }

    override fun toString(e: Event?, debug: Boolean): String = ""
}
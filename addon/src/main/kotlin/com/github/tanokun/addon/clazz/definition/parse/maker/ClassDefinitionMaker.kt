package com.github.tanokun.addon.clazz.definition.parse.maker

import ch.njol.skript.Skript
import ch.njol.skript.lang.Literal
import ch.njol.skript.lang.SelfRegisteringSkriptEvent
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.Trigger
import com.github.tanokun.addon.maker.ClassDefinitionBukkitEvent
import org.bukkit.event.Event

class ClassDefinitionMaker : SelfRegisteringSkriptEvent() {
    companion object {
        init {
            Skript.registerEvent("*class", ClassDefinitionMaker::class.java, arrayOf(ClassDefinitionBukkitEvent::class.java), "class %identifier%")
        }
    }

    override fun init(
        args: Array<out Literal<*>>,
        matchedPattern: Int,
        parseResult: SkriptParser.ParseResult,
    ): Boolean = true

    override fun toString(e: Event?, debug: Boolean): String? = "class"

    override fun register(t: Trigger) {}

    override fun unregister(t: Trigger) {}

    override fun unregisterAll() {}
}
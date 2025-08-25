package com.github.tanokun.addon.instance.observe

import ch.njol.skript.Skript
import ch.njol.skript.classes.ClassInfo
import ch.njol.skript.config.SectionNode
import ch.njol.skript.lang.Literal
import ch.njol.skript.lang.SelfRegisteringSkriptEvent
import ch.njol.skript.lang.SkriptEvent
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.Trigger
import ch.njol.skript.log.ErrorQuality
import ch.njol.skript.log.SkriptLogger
import ch.njol.skript.registrations.Classes
import com.github.tanokun.addon.clazz.ClassRegistry
import com.github.tanokun.addon.clazz.definition.Identifier
import com.github.tanokun.addon.clazz.definition.parse.ParsedClassContentsCombinator
import com.github.tanokun.addon.clazz.definition.parse.currentlyCombinator
import com.github.tanokun.addon.clazz.definition.parse.register.ClassDefinitionRegister
import com.github.tanokun.addon.clazz.loader.StaticClassDefinitionLoader
import com.github.tanokun.addon.instance.AnyInstance
import com.github.tanokun.addon.maker.ClassDefinitionBukkitEvent
import com.github.tanokun.addon.maker.observe.ObserveInstanceBukkitEvent
import jdk.nashorn.internal.codegen.CompilerConstants.className
import org.bukkit.event.Event
import kotlin.jvm.java

class ObserveInstanceEvent : SkriptEvent() {
    companion object {
        init {
            val classes = StaticClassDefinitionLoader.loadedClasses
                .map { it.simpleName }

            Skript.registerEvent("*observe", ObserveInstanceEvent::class.java, arrayOf(ObserveInstanceBukkitEvent::class.java), "observe (${classes.reduceOrNull { acc, className -> "$acc|$className" }})")
        }
    }

    private lateinit var className: Identifier

    override fun init(
        args: Array<out Literal<*>>,
        matchedPattern: Int,
        parseResult: SkriptParser.ParseResult,
    ): Boolean {
        className = Identifier(parseResult.expr.split(" ", limit = 2)[0])


        return true
    }

    override fun check(e: Event): Boolean = true


    override fun toString(e: Event?, debug: Boolean): String? = "class"
}
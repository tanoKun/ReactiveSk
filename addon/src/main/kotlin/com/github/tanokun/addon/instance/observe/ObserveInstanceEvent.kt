package com.github.tanokun.addon.instance.observe

import ch.njol.skript.lang.Literal
import ch.njol.skript.lang.SkriptEvent
import ch.njol.skript.lang.SkriptParser
import com.github.tanokun.addon.clazz.definition.Identifier
import org.bukkit.event.Event

class ObserveInstanceEvent : SkriptEvent() {
    companion object {
/*        init {
            val classes = StaticClassDefinitionLoader.loadedClasses
                .map { it.simpleName }

            Skript.registerEvent("*observe", ObserveInstanceEvent::class.java, arrayOf(ObserveInstanceBukkitEvent::class.java), "observe (${classes.reduceOrNull { acc, className -> "$acc|$className" }})")
        }*/
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
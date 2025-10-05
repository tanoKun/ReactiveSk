package com.github.tanokun.reactivesk.v263.skript.resolve.clazz

import ch.njol.skript.Skript
import ch.njol.skript.config.SectionNode
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.Section
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.TriggerItem
import ch.njol.util.Kleenean
import com.github.tanokun.reactivesk.v263.skript.resolve.patch.CurrentClassParserPatch.currentResolvingClass
import org.bukkit.event.Event

class FieldDefinitionMakerSection: Section() {
    companion object {
        fun register() {
            Skript.registerSection(FieldDefinitionMakerSection::class.java, "field")
        }
    }

    override fun init(
        exprs: Array<out Expression<*>>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult,
        sectionNode: SectionNode,
        triggerItems: List<TriggerItem>,
    ): Boolean {
        if (parser.currentResolvingClass == null) {
            Skript.error("'field' definition can only be used inside a 'class' section.")
            return false
        }

        return true
    }

    override fun walk(e: Event?): TriggerItem? { return null }

    override fun toString(e: Event?, debug: Boolean): String = "field definition section"
}
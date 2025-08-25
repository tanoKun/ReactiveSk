package com.github.tanokun.addon.clazz.definition.parse.register

import ch.njol.skript.ScriptLoader
import ch.njol.skript.Skript
import ch.njol.skript.classes.ClassInfo
import ch.njol.skript.config.SectionNode
import ch.njol.skript.lang.Literal
import ch.njol.skript.lang.SelfRegisteringSkriptEvent
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.Trigger
import ch.njol.skript.log.ErrorQuality
import ch.njol.skript.log.SkriptLogger
import ch.njol.skript.registrations.Classes
import com.github.tanokun.addon.clazz.definition.ClassDefinition
import com.github.tanokun.addon.clazz.ClassRegistry
import com.github.tanokun.addon.clazz.definition.Identifier
import com.github.tanokun.addon.clazz.definition.function.FunctionDefinition
import com.github.tanokun.addon.clazz.definition.parse.ParsedClassContentsCombinator
import com.github.tanokun.addon.clazz.definition.parse.currentlyCombinator
import com.github.tanokun.addon.clazz.definition.parse.field.ParseFieldResult
import com.github.tanokun.addon.clazz.definition.parse.function.ParseFunctionResult
import com.github.tanokun.addon.clazz.definition.parse.function.FunctionParser
import com.github.tanokun.addon.clazz.definition.parse.function.register.FunctionDefinitionRegister
import com.github.tanokun.addon.instance.AnyInstance
import com.github.tanokun.addon.instance.call.FunctionReturnEffect
import com.github.tanokun.addon.maker.ClassDefinitionBukkitEvent
import org.bukkit.event.Event

class ClassDefinitionRegister : SelfRegisteringSkriptEvent() {
    companion object {
        init {
            Skript.registerEvent("*class", ClassDefinitionRegister::class.java, arrayOf(ClassDefinitionBukkitEvent::class.java), "class %identifier%")
        }
    }

    override fun init(
        args: Array<out Literal<*>>,
        matchedPattern: Int,
        parseResult: SkriptParser.ParseResult,
    ): Boolean {
        val node = SkriptLogger.getNode() ?: return true

        val className = node.key?.split(" ")[1] ?: run {
            Skript.error("クラスの名前が指定されていません。", ErrorQuality.SEMANTIC_ERROR)
            return false
        }

        (node as SectionNode).firstOrNull { it.key?.trim() == "field" } as? SectionNode ?: run {
            Skript.warning("クラス '$className' に 'field:' が存在しません。")
            return false
        }

        currentlyCombinator = ParsedClassContentsCombinator(
            ClassRegistry, Identifier(className), Classes.getClassInfo(className.lowercase()) as ClassInfo<out AnyInstance>
        )

        return true
    }

    override fun toString(e: Event?, debug: Boolean): String? = "class"

    override fun register(t: Trigger) {}

    override fun unregister(t: Trigger) {}

    override fun unregisterAll() {}
}
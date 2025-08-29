package com.github.tanokun.addon.definition.skript.dynamic

import ch.njol.skript.Skript
import ch.njol.skript.lang.Literal
import ch.njol.skript.lang.SkriptEvent
import ch.njol.skript.lang.SkriptParser
import com.github.tanokun.addon.definition.dynamic.ClassDefinition
import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.dynamicClassDefinitionLoader
import com.github.tanokun.addon.definition.skript.maker.ClassDefinitionEventMaker
import com.github.tanokun.addon.dynamicJavaClassLoader
import com.github.tanokun.addon.definition.dynamic.DynamicClass
import org.bukkit.event.Event

/**
 * Skript の class セクションを解釈するための疑似イベントです。
 * 動的に生成されたクラスを解決し、セクション内で参照できるようにします。
 * @property dynamicClass 解決された動的クラス
 */
class ClassDefinitionSkriptEvent : SkriptEvent() {
    var dynamicClassDefinition: ClassDefinition? = null
        private set

    var dynamicClass: Class<out DynamicClass>? = null
        private set

    companion object {
        init {
            Skript.registerEvent(
                "*class",
                ClassDefinitionSkriptEvent::class.java,
                arrayOf(ClassDefinitionEventMaker::class.java),
                "class <.+>"
            )
        }
    }

    override fun init(
        args: Array<out Literal<*>>,
        matchedPattern: Int,
        parseResult: SkriptParser.ParseResult,
    ): Boolean {
        val className = parseResult.expr.split(" ", limit = 3)[1].split("[", limit = 2)[0]
        val classNameIdentifier = Identifier(className)

        dynamicClassDefinition = dynamicClassDefinitionLoader.getClassDefinition(classNameIdentifier)
        dynamicClass = dynamicJavaClassLoader.getDynamicClassOrGenerate(classNameIdentifier)

        return true
    }


    override fun check(e: Event?): Boolean = false

    override fun toString(e: Event?, debug: Boolean): String? = "class definition event"

    override fun hashCode(): Int = dynamicClassDefinition?.hashCode() ?: 0

    override fun equals(other: Any?): Boolean = this === other
}
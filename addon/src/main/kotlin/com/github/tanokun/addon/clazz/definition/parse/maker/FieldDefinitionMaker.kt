package com.github.tanokun.addon.clazz.definition.parse.maker

import ch.njol.skript.Skript
import ch.njol.skript.config.SectionNode
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.Section
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.TriggerItem
import ch.njol.skript.log.ErrorQuality
import ch.njol.util.Kleenean
import com.github.tanokun.addon.maker.ClassDefinitionBukkitEvent
import org.bukkit.event.Event

class FieldDefinitionMaker: Section() {
    companion object {
        init {
            Skript.registerSection(FieldDefinitionMaker::class.java, "field")
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
        val currentEvents = parser.currentEvents
        if (currentEvents.none { it == ClassDefinitionBukkitEvent::class.java }) {
            Skript.error("'field' 定義は 'class' ブロックの内部でのみ使用できます。", ErrorQuality.SEMANTIC_ERROR)
            return false
        }

/*        val currentlyCombinator = currentlyCombinator ?: return false
        val className = currentlyCombinator.className

        val fieldParser = FieldDefinitionParser(sectionNode)
        val fieldDefinitions = fieldParser.parseField().mapNotNull { result ->
            when (result) {
                is ParseFieldResult.Success -> {
                    val classInfo = Classes.getClassInfoNoError(result.unidentifiedClassDefinition.type.c.simpleName.lowercase())
                    if (classInfo == null) {
                        Skript.error("クラス '$className' のフィールド '${result.unidentifiedClassDefinition.fieldName}' の型 '${result.unidentifiedClassDefinition.type.c.simpleName}' は存在しません。", ErrorQuality.SEMANTIC_ERROR)
                        null
                    }

                    result.unidentifiedClassDefinition
                }
                is ParseFieldResult.Failure -> {
                    Skript.error("クラス '$className' のフィールド定義にエラーがあります: ${result.errorMessage}", ErrorQuality.SEMANTIC_ERROR)
                    null
                }
            }
        }

        currentlyCombinator.fields = fieldDefinitions*/

        return true
    }

    override fun walk(e: Event?): TriggerItem? { return null }

    override fun toString(e: Event?, debug: Boolean): String? = "field definition fake section"
}
package com.github.tanokun.reactivesk.skriptadapter.v2_6_3.skript.resolve

import ch.njol.skript.Skript
import ch.njol.skript.lang.Literal
import ch.njol.skript.lang.SkriptEvent
import ch.njol.skript.lang.SkriptParser
import com.github.tanokun.reactivesk.lang.Identifier
import com.github.tanokun.reactivesk.skriptadapter.common.dynamic.DynamicClassDefinitionLoader
import com.github.tanokun.reactivesk.skriptadapter.common.dynamic.DynamicManager
import com.github.tanokun.reactivesk.skriptadapter.common.skript.CurrentClassParserPatch.currentResolvingClass
import com.github.tanokun.reactivesk.skriptadapter.common.skript.ResolvingClass
import org.bukkit.event.Event
import org.koin.java.KoinJavaComponent.inject

/**
 * Skript の class セクションを解釈するための疑似イベントです。
 * 動的に生成されたクラスを解決し、セクション内で参照できるようにします。
 * @property dynamicClass 解決された動的クラス
 */
class SetCurrentResolvingClassSkriptEvent : SkriptEvent() {

    private val dynamicManager: DynamicManager by inject(DynamicManager::class.java)

    private val definitionLoader: DynamicClassDefinitionLoader by inject(DynamicClassDefinitionLoader::class.java)

    companion object {
        fun register() {
            Skript.registerEvent(
                "*class",
                SetCurrentResolvingClassSkriptEvent::class.java,
                arrayOf(),
                "class <.+>"
            )
        }
    }

    override fun init(
        args: Array<out Literal<*>>,
        matchedPattern: Int,
        parseResult: SkriptParser.ParseResult,
    ): Boolean {
        // testClass[val test: String]
        val classNameIdentifier = parseResult.expr
            .split(" ", limit = 3)[1].split("[", limit = 2)[0]
            .let { return@let Identifier(it) }

        val dynamicClassDefinition = definitionLoader.getClassDefinition(classNameIdentifier) ?:  throw IllegalStateException("Cannot find class '$classNameIdentifier'.")
        val dynamicClass = dynamicManager.getLoadedClass(classNameIdentifier) ?: throw IllegalStateException("Cannot find class '$classNameIdentifier'.")

        parser.currentResolvingClass = ResolvingClass(dynamicClassDefinition, dynamicClass)

        return true
    }

    override fun check(e: Event?): Boolean = false

    override fun toString(e: Event?, debug: Boolean): String = "class definition event"
}
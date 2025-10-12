package com.github.tanokun.reactivesk.v263.skript.resolve.clazz

import ch.njol.skript.Skript
import ch.njol.skript.lang.Literal
import ch.njol.skript.lang.SkriptEvent
import ch.njol.skript.lang.SkriptParser
import com.github.tanokun.reactivesk.lang.Identifier
import com.github.tanokun.reactivesk.v263.ReactiveSkAddon
import com.github.tanokun.reactivesk.v263.skript.resolve.patch.CurrentClassParserPatch.currentResolvingClass
import com.github.tanokun.reactivesk.v263.skript.resolve.patch.ResolvingClass
import org.bukkit.event.Event

/**
 * Skript の class セクションを解釈するための疑似イベントです。
 * 動的に生成されたクラスを解決し、セクション内で参照できるようにします。
 */
class SetCurrentResolvingClassSkriptEvent : SkriptEvent() {

    private val dynamicManager = ReactiveSkAddon.dynamicManager

    private val definitionLoader = ReactiveSkAddon.definitionLoader

    companion object {
        fun register() {
            Skript.registerEvent("*class", SetCurrentResolvingClassSkriptEvent::class.java, arrayOf(), "class <.+>")
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

        val dynamicClassDefinition = definitionLoader.getClassDefinition(classNameIdentifier) ?: throw IllegalStateException("Cannot find class '$classNameIdentifier'.")
        val dynamicClass = dynamicManager.getLoadedClass(classNameIdentifier) ?: throw IllegalStateException("Cannot find class '$classNameIdentifier'.")

        parser.currentResolvingClass = ResolvingClass(dynamicClassDefinition, dynamicClass)

        return true
    }

    override fun check(e: Event?): Boolean = false

    override fun toString(e: Event?, debug: Boolean): String = "class definition event"
}
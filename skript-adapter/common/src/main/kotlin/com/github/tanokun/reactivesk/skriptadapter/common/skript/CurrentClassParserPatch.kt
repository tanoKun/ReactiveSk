package com.github.tanokun.reactivesk.skriptadapter.common.skript

import ch.njol.skript.lang.parser.ParserInstance
import java.util.*

object CurrentClassParserPatch {
    private val currentResolvingClasses = WeakHashMap<ParserInstance, ResolvingClass>()

    var ParserInstance.currentResolvingClass: ResolvingClass?
        get() = currentResolvingClasses[this]
        set(value) {
            if (value == null) currentResolvingClasses.remove(this)
            else currentResolvingClasses[this] = value
        }
}
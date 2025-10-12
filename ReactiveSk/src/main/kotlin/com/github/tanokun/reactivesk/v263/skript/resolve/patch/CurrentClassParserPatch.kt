package com.github.tanokun.reactivesk.v263.skript.resolve.patch

import ch.njol.skript.config.Node
import ch.njol.skript.lang.parser.ParserInstance
import java.util.*

object CurrentClassParserPatch {
    private val currentResolvingClasses = WeakHashMap<Node, ResolvingClass>()

    var ParserInstance.currentResolvingClass: ResolvingClass?
        get() = currentResolvingClasses[getClassSection()]
        set(value) {
            val node = getClassSection() ?: return

            if (value == null) currentResolvingClasses.remove(node)
            else currentResolvingClasses[node] = value
        }

    private fun ParserInstance.getClassSection(): Node? {
        var current = this.node ?: return null

        while (true) {
            if (current.key?.startsWith("class ") == true) return current

            current.parent?.let {
                current = it
                continue
            }

            return current
        }

    }
}
package com.github.tanokun.addon.intermediate.parse.ast

import ch.njol.skript.lang.Section
import ch.njol.skript.lang.TriggerItem
import ch.njol.skript.sections.SecConditional
import ch.njol.skript.sections.SecLoop
import com.github.tanokun.addon.intermediate.Reflection

interface SkriptEnv {
    fun conditionalType(cond: SecConditional): CType
    fun sectionNext(section: Section): TriggerItem?
    fun findIfChain(ifHeader: SecConditional): IfChain
}

data class IfChain(
    val ifHeader: SecConditional,
    val elseIfs: List<SecConditional>,
    val elseNode: SecConditional?
)

object DefaultSkriptEnv : SkriptEnv {
    override fun conditionalType(cond: SecConditional): CType {
        val field = Reflection.findField(cond.javaClass, "type")
        val rawValue = field.get(cond) as Enum<*>
        return CType.valueOf(rawValue.name)
    }

    override fun sectionNext(section: Section): TriggerItem? {
        return if (section is SecLoop) {
            section.actualNext
        } else {
            section.next
        }
    }

    override fun findIfChain(ifHeader: SecConditional): IfChain {
        val elseIfs = mutableListOf<SecConditional>()
        var elseNode: SecConditional? = null
        var nextLink = ifHeader.normalNext

        while (nextLink is SecConditional) {
            when (conditionalType(nextLink)) {
                CType.ELSE_IF -> {
                    elseIfs.add(nextLink)
                    nextLink = nextLink.normalNext
                }
                CType.ELSE -> {
                    elseNode = nextLink
                    break
                }
                CType.IF -> break
            }
        }
        return IfChain(ifHeader, elseIfs, elseNode)
    }
}

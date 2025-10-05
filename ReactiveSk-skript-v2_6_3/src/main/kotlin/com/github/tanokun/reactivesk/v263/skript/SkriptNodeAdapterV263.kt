package com.github.tanokun.reactivesk.v263.skript

import ch.njol.skript.ScriptLoader
import ch.njol.skript.config.SectionNode
import ch.njol.skript.lang.TriggerItem
import com.github.tanokun.reactivesk.skriptadapter.common.SkriptNodeAdapter

object SkriptNodeAdapterV263: SkriptNodeAdapter<SectionNode, TriggerItem> {
    override fun loadFromSectionNode(sectionNode: SectionNode): List<TriggerItem> {
        val items = ScriptLoader.loadItems(sectionNode)

        if (items.isNotEmpty())
            for (i in 0 until items.size - 1) {
                items[i].next = items[i + 1]
            }

        return items
    }

    override fun getNext(node: TriggerItem): TriggerItem? = node.next
}
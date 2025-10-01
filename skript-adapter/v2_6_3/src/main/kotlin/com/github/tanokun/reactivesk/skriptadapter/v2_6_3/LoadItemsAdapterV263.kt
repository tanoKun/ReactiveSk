package com.github.tanokun.reactivesk.skriptadapter.v2_6_3

import ch.njol.skript.ScriptLoader
import ch.njol.skript.config.SectionNode
import ch.njol.skript.lang.TriggerItem
import com.github.tanokun.reactivesk.skriptadapter.common.LoadItemsAdapter
import org.koin.core.annotation.Single

@Single(binds = [LoadItemsAdapter::class])
object LoadItemsAdapterV263: LoadItemsAdapter {
    override fun loadFromSectionNode(sectionNode: SectionNode): List<TriggerItem> {
        val items = ScriptLoader.loadItems(sectionNode)

        if (items.isNotEmpty())
            for (i in 0 until items.size - 1) {
                items[i].next = items[i + 1]
            }

        return items
    }
}
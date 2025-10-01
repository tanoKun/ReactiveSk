package com.github.tanokun.reactivesk.skriptadapter.common

import ch.njol.skript.config.SectionNode
import ch.njol.skript.lang.TriggerItem

interface LoadItemsAdapter {
    fun loadFromSectionNode(sectionNode: SectionNode): List<TriggerItem>
}
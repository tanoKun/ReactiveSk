package com.github.tanokun.addon.maker.observe

import com.github.tanokun.addon.instance.AnyInstance
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class ObserveInstanceBukkitEvent(
    val parent: AnyInstance,
    val targetPropertyName: String
): Event() {
    companion object {
        private val _handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = _handlers
    }

    override fun getHandlers(): HandlerList = _handlers
}
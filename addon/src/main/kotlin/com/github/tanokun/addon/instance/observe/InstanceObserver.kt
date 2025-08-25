package com.github.tanokun.addon.instance.observe

import com.github.tanokun.addon.clazz.ClassRegistry
import com.github.tanokun.addon.coroutineScope
import com.github.tanokun.addon.instance.AnyInstance
import com.github.tanokun.addon.instance.InstanceProperty
import com.github.tanokun.addon.maker.observe.ObserveInstanceBukkitEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.bukkit.Bukkit

class InstanceObserver(private val instance: AnyInstance) {
    init {
        instance
    }

    fun publish(propertyName: String) {
        Bukkit.getPluginManager().callEvent(ObserveInstanceBukkitEvent(instance, propertyName))
    }
}
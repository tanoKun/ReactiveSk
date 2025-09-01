package com.github.tanokun.addon.runtime.skript.observe.mediator

import com.github.tanokun.addon.definition.dynamic.DynamicClass
import com.github.tanokun.addon.runtime.skript.observe.ObserverSkriptEvent
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class RuntimeObservingMediator(): Event() {
    override fun getHandlers(): HandlerList? = null

    companion object {
        private val observers = hashMapOf<Class<out DynamicClass>, ArrayList<ObserverSkriptEvent>>()

        fun register(observer: ObserverSkriptEvent) {
            observers.computeIfAbsent(observer.dynamicClassInfo.clazz) { arrayListOf() }.add(observer)
        }

        fun unregister(observer: ObserverSkriptEvent) {
            observers[observer.dynamicClassInfo.clazz]?.remove(observer)
        }

        fun unregisterAll() {
            observers.clear()
        }

        fun execute(any: Any, old: Any?, new: Any?, factor: String) {
            val observers = observers[any::class.java] ?: return

            observers
                .filter { it.factor == factor }
                .forEach {
                    it.execute(RuntimeObservingMediator(), any, old, new)
                }
        }
    }
}
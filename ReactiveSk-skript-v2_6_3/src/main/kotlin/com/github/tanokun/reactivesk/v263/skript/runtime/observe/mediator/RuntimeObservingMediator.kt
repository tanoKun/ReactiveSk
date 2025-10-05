package com.github.tanokun.reactivesk.v263.skript.runtime.observe.mediator

import com.github.tanokun.reactivesk.v263.skript.DynamicClass
import com.github.tanokun.reactivesk.v263.skript.runtime.observe.ObserverSkriptEvent
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class RuntimeObservingMediator(): Event() {
    override fun getHandlers(): HandlerList? = null

    companion object {
        private val observers = hashMapOf<Class<out DynamicClass>, ArrayList<ObserverSkriptEvent>>()

        fun register(observer: ObserverSkriptEvent) {
            observers
                .getOrPut(observer.dynamicClassInfo.clazz) { arrayListOf() }
                .add(observer)
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
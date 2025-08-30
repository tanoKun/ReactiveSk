package com.github.tanokun.addon.runtime.variable

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.bukkit.event.Event
import java.util.concurrent.ConcurrentMap

object VariableFrames {
    val cache: Cache<Event, Array<Any?>> =
        CacheBuilder.newBuilder()
            .weakKeys()
            .build()

    val frames: ConcurrentMap<Event, Array<Any?>> = cache.asMap()

    @JvmStatic
    fun beginFrame(event: Event, capacity: Int) {
        frames[event] = arrayOfNulls(capacity)
    }

    @JvmStatic
    fun endFrame(event: Event) {
        frames.remove(event)
    }

    @JvmStatic
    fun get(event: Event, index: Int): Any? {
        val arr = frames[event] ?: error("frame missing for event")
        return arr[index]
    }

    @JvmStatic
    fun set(event: Event, index: Int, value: Any?) {
        val arr = frames[event] ?: error("frame missing for event")
        arr[index] = value
    }
}

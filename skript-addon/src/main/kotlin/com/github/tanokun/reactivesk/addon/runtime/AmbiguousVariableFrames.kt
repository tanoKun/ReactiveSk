package com.github.tanokun.reactivesk.addon.runtime

import java.util.*

object AmbiguousVariableFrames {
    val frames: WeakHashMap<Any, ArrayList<Any?>> = WeakHashMap()

    @JvmStatic
    fun beginFrame(event: Any, capacity: Int = 10) {
        frames[event] = arrayListOf(capacity)
    }

    @JvmStatic
    fun endFrame(event: Any) {
        frames.remove(event)
    }

    @JvmStatic
    fun get(event: Any, index: Int): Any? {
        val arr = frames[event] ?: error("frame missing for event")
        return arr[index]
    }

    @JvmStatic
    fun set(event: Any, index: Int, value: Any?) {
        val arr = frames[event] ?: error("frame missing for event")
        arr[index] = value
    }
}
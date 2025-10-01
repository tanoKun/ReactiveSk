package com.github.tanokun.addon.runtime.variable

import java.util.*

object AmbiguousVariableFrames {
    val frames: WeakHashMap<Any, ArrayList<Any?>> = WeakHashMap()

    @JvmStatic
    fun beginFrame(event: Any) {
        frames[event] = arrayListOf()
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
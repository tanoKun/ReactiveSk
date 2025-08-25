package com.github.tanokun.addon.maker.function

import org.bukkit.event.Event

sealed class FunctionRuntimeBukkitEvent: Event() {

    abstract fun setReturn(any: Any?)

    abstract suspend fun getReturn(): Any?
}
package com.github.tanokun.addon.maker.function

import org.bukkit.event.HandlerList
import java.util.UUID

class NonSuspendFunctionRuntimeBukkitEvent(private val id: UUID = UUID.randomUUID()): FunctionRuntimeBukkitEvent() {
    private var returns: Any? = null

    override fun getHandlers(): HandlerList? = null

    override fun setReturn(any: Any?) {
        returns = any
    }

    override suspend fun getReturn(): Any? = returns
}
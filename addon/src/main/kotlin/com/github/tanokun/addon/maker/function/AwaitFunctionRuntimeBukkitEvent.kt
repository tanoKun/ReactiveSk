package com.github.tanokun.addon.maker.function

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import java.util.UUID

class AwaitFunctionRuntimeBukkitEvent(private val id: UUID = UUID.randomUUID()): FunctionRuntimeBukkitEvent() {
    private val returns: MutableSharedFlow<Any?> = MutableSharedFlow(replay = 1)

    override fun getHandlers(): HandlerList? = null

    override fun setReturn(any: Any?) {
        returns.tryEmit(any)
    }

    override suspend fun getReturn(): Any? = returns.first()
}
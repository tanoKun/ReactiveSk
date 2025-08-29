package com.github.tanokun.addon.runtime.skript.function.call.mediator

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import org.bukkit.event.HandlerList

class AwaitRuntimeFunctionMediator: RuntimeFunctionMediator() {
    private val returns: MutableSharedFlow<Any?> = MutableSharedFlow(replay = 1)

    override fun getHandlers(): HandlerList? = null

    override fun setReturn(any: Any?) {
        returns.tryEmit(any)
    }

    override suspend fun getReturn(): Any? = returns.first()
}
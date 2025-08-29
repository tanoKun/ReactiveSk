package com.github.tanokun.addon.runtime.skript.function.call.mediator

import org.bukkit.event.HandlerList

class NonSuspendRuntimeFunctionMediator: RuntimeFunctionMediator() {
    private var returns: Any? = null

    override fun getHandlers(): HandlerList? = null

    override fun setReturn(any: Any?) {
        returns = any
    }

    override suspend fun getReturn(): Any? = returns
}
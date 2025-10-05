package com.github.tanokun.reactivesk.v263.skript.runtime.function.mediator

import org.bukkit.event.HandlerList

class NonSuspendRuntimeFunctionMediator: RuntimeFunctionMediator() {
    private var returns: Any? = null

    override fun getHandlers(): HandlerList? = null

    override fun setReturn(any: Any?) {
        returns = any
    }

    override suspend fun getReturn(): Any? = returns

    fun nonSuspendGetReturn(): Any? = returns
}
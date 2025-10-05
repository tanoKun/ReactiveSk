package com.github.tanokun.reactivesk.v263.skript.runtime.function.mediator

import org.bukkit.event.Event

sealed class RuntimeFunctionMediator(): Event() {

    abstract fun setReturn(any: Any?)

    abstract suspend fun getReturn(): Any?

    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int = javaClass.hashCode()
}
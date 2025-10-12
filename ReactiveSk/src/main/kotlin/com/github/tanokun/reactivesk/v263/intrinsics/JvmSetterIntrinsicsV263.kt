package com.github.tanokun.reactivesk.v263.intrinsics

import com.github.tanokun.reactivesk.compiler.backend.intrinsics.JvmSetterIntrinsics
import com.github.tanokun.reactivesk.v263.ReactiveSkAddon
import com.github.tanokun.reactivesk.v263.skript.runtime.observe.mediator.RuntimeObservingMediator
import kotlinx.coroutines.launch

object JvmSetterIntrinsicsV263: JvmSetterIntrinsics {
    override fun notifyChanged(notified: Boolean, instance: Any, propertyName: String, oldValue: Any?, newValue: Any?) {
        if (!notified) return
        if (oldValue == newValue) return

        ReactiveSkAddon.coroutineScope.launch {
            RuntimeObservingMediator.execute(instance, oldValue, newValue, propertyName)
        }
    }

    override fun checkTypes(list: ArrayList<*>, expected: Class<*>) {
        list.forEach {
            if (it != null && !expected.isAssignableFrom(it.javaClass)) {
                error("Type mismatch: expected ${expected.name}, but got ${it.javaClass.name}")
            }
        }
    }
}
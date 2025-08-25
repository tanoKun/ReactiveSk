package com.github.tanokun.addon.instance

import com.sun.xml.internal.ws.api.ha.StickyFeature
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.subscribe

data class InstanceProperty(
    var propertyName: String = "",
    var parentClass: String = "",
    private var _value: Any? = null
) {

    val value: Any? get() = _value

    private val _flow = MutableSharedFlow<Any?>()

    val flow: SharedFlow<Any?> = _flow

    suspend fun emit(any: Any?) {
        _value = any
        _flow.emit(any)
    }
}
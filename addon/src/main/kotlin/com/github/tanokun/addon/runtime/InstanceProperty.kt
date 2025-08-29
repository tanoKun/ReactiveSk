package com.github.tanokun.addon.runtime

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

data class InstanceProperty(
    var propertyName: String = "",
    var parentClass: String = "",
    var isArray: Boolean = false,
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
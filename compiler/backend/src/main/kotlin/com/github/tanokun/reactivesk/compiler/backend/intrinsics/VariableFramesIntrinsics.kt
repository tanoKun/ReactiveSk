package com.github.tanokun.reactivesk.compiler.backend.intrinsics

interface VariableFramesIntrinsics {

    fun beginFrame(mediator: Any, capacity: Int = 10)

    fun set(mediator: Any, index: Int, value: Any?)
}
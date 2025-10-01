package com.github.tanokun.reactivesk.compiler.backend.intrinsics

interface TriggerItemIntrinsics {
    fun walk(trigger: Any?, event: Any)
}
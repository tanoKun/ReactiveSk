package com.github.tanokun.reactivesk.compiler.backend

import com.github.tanokun.reactivesk.compiler.backend.intrinsics.JvmSetterIntrinsics

object TestHelpers: JvmSetterIntrinsics {

    override fun notifyChanged(notified: Boolean, instance: Any, propertyName: String, oldValue: Any?, newValue: Any?, ) {
    }

    override fun checkTypes(list: ArrayList<*>, expected: Class<*>) {
    }
}
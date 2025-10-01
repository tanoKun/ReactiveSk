package com.github.tanokun.reactivesk.skriptadapter.common.dynamic

import com.github.tanokun.reactivesk.lang.Identifier

interface DynamicManager {

    fun initialize()

    fun reload(): List<ChangedDifference>

    fun getLoadedClass(typeName: Identifier): Class<*>?
}
package com.github.tanokun.reactivesk.skriptadapter.common.skript

import com.github.tanokun.reactivesk.lang.ClassDefinition

data class ResolvingClass(
    val definition: ClassDefinition,
    val clazz: Class<*>
)

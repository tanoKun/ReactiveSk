package com.github.tanokun.reactivesk.v263.skript.resolve.patch

import com.github.tanokun.reactivesk.lang.ClassDefinition
import com.github.tanokun.reactivesk.v263.skript.DynamicClass

data class ResolvingClass(
    val definition: ClassDefinition,
    val clazz: Class<out DynamicClass>
)
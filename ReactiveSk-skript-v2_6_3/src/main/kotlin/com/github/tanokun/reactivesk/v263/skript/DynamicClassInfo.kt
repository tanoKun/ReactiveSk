package com.github.tanokun.reactivesk.v263.skript

import com.github.tanokun.reactivesk.lang.ClassDefinition

data class DynamicClassInfo(val classDefinition: ClassDefinition, val clazz: Class<out DynamicClass>)
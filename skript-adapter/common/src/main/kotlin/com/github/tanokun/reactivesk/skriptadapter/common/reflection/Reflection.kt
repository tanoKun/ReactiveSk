package com.github.tanokun.reactivesk.skriptadapter.common.reflection

import java.lang.reflect.Field

object Reflection {
    private val fieldCache = mutableMapOf<Pair<Class<*>, String>, Field>()

    fun findField(cls: Class<*>, name: String): Field = fieldCache.getOrPut(cls to name) {
        var currentClass: Class<*>? = cls
        while (currentClass != null) {
            try {
                return@getOrPut currentClass.getDeclaredField(name).apply { isAccessible = true }
            } catch (_: NoSuchFieldException) {
                currentClass = currentClass.superclass
            }
        }
        throw NoSuchFieldException("Field '$name' not found in ${cls.name} or its superclasses")
    }
}
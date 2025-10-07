package com.github.tanokun.reactivesk.v263.skript.serializer

import ch.njol.skript.classes.Serializer
import ch.njol.yggdrasil.Fields
import com.github.tanokun.reactivesk.compiler.backend.asFqcn
import com.github.tanokun.reactivesk.compiler.backend.codegen.util.FIELD_PREFIX
import com.github.tanokun.reactivesk.lang.Identifier
import com.github.tanokun.reactivesk.v263.skript.DynamicClass


class DynamicInstanceSerializer<T: DynamicClass>: Serializer<T>() {
    override fun serialize(o: T): Fields? {
        val fields = Fields()

        val expectName = Identifier(o::class.simpleName ?: "").asFqcn()
        val clazz = o::class.java

        if (clazz.name != expectName) return null

        clazz.declaredFields
            .filter { it.name.startsWith(FIELD_PREFIX) }
            .onEach { it.isAccessible = true }
            .forEach { fields.putObject(it.name, it.get(o)) }

        return fields
    }

    override fun deserialize(o: T, fields: Fields) {
        val expectName = Identifier(o::class.simpleName ?: "").asFqcn()
        val clazz = o::class.java

        if (clazz.name != expectName) return

        try {
            fields.forEach {
                clazz.getField(it.id).set(o, it.`object`)
            }
        } catch (_: Throwable) { }
    }

    override fun mustSyncDeserialization(): Boolean = false

    override fun canBeInstantiated(): Boolean = true
}

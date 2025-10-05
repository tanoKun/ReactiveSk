package com.github.tanokun.reactivesk.v263.skript.serializer

import ch.njol.skript.classes.Serializer
import ch.njol.yggdrasil.Fields
import com.github.tanokun.reactivesk.compiler.backend.codegen.util.FIELD_PREFIX
import com.github.tanokun.reactivesk.v263.skript.DynamicClass


class DynamicInstanceSerializer<T: DynamicClass>: Serializer<T>() {
    override fun serialize(o: T): Fields? {
        val fields = Fields()

        if (!o::class.java.name.startsWith("com.github.tanokun.addon.generated.")) {
            return null
        }

        o::class.java.declaredFields
            .filter { it.name.startsWith(FIELD_PREFIX) }
            .onEach { it.isAccessible = true }
            .forEach { fields.putObject(it.name, it.get(o)) }

        return fields
    }

    override fun deserialize(o: T, fields: Fields) {
        if (!o::class.java.name.startsWith("com.github.tanokun.addon.generated.")) {
            return
        }

        try {
            fields.forEach {
                o::class.java.getField(it.id).set(o, it.`object`)
            }
        } catch (_: Throwable) { }
    }

    override fun mustSyncDeserialization(): Boolean = false

    override fun canBeInstantiated(): Boolean = true
}

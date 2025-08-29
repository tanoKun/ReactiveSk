package com.github.tanokun.addon.runtime.skript.serializer

import ch.njol.skript.classes.Serializer
import ch.njol.yggdrasil.Fields
import com.github.tanokun.addon.intermediate.generator.FIELD_PREFIX
import com.github.tanokun.addon.definition.dynamic.DynamicClass


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

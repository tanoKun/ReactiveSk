package com.github.tanokun.addon.instance.serializer

import ch.njol.skript.classes.Serializer
import ch.njol.yggdrasil.Fields
import com.github.tanokun.addon.instance.InstanceProperty
import kotlinx.coroutines.runBlocking


class InstancePropertySerializer: Serializer<InstanceProperty>() {
    override fun serialize(target: InstanceProperty): Fields {
        val fields = Fields()

        fields.putObject("propertyName", target.propertyName)
        fields.putObject("parentClassName", target.parentClass)
        fields.putObject("value", target.value)

        return fields

    }

    override fun deserialize(any: InstanceProperty, fields: Fields) {
        any.propertyName = fields.getObject("propertyName").toString()
        any.parentClass = fields.getObject("parentClassName").toString()
        runBlocking { any.emit(fields.getObject("value")) }
    }

    override fun mustSyncDeserialization(): Boolean = false

    override fun canBeInstantiated(): Boolean = true
}

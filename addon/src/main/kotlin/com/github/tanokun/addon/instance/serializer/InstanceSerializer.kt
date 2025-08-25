package com.github.tanokun.addon.instance.serializer

import ch.njol.skript.classes.Serializer
import ch.njol.yggdrasil.Fields
import com.github.tanokun.addon.instance.AnyInstance
import com.github.tanokun.addon.instance.InstanceProperty
import kotlin.text.toInt


class InstanceSerializer<T: AnyInstance>(): Serializer<T>() {
    override fun serialize(o: T): Fields {
        val fields = Fields()

        fields.putObject("className", o.className)
        fields.putObject("properties", o.properties)

        return fields
    }

    override fun deserialize(any: T, fields: Fields) {
        any.className = fields.getObject("className").toString()
        any.properties = fields.getObject("properties") as Collection<InstanceProperty>
    }

    override fun mustSyncDeserialization(): Boolean = false

    override fun canBeInstantiated(): Boolean = true
}

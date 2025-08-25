package com.github.tanokun.addon.clazz.definition.field

import com.github.tanokun.addon.clazz.definition.Identifier
import com.github.tanokun.addon.instance.AnyInstance
import com.github.tanokun.addon.instance.InstanceProperty

data class FieldDefinition(
    val fieldName: Identifier,
    val typeName: String,
    val isMutable: Boolean,
    val isArray: Boolean,
) {

    fun newInstance(parentClassName: String, value: Any): InstanceProperty {
        if (value::class.java.simpleName.uppercase() == typeName.uppercase()) {
            return InstanceProperty(fieldName.identifier, parentClassName, value)
        }

        if (value is AnyInstance && value.className == typeName) {
            return InstanceProperty(fieldName.identifier, parentClassName, value)
        }

        throw IllegalArgumentException("型が一致しません: expected=$typeName, actual=${value::class.java.simpleName}")
    }
}
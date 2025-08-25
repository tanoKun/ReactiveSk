package com.github.tanokun.addon.clazz.definition.field

import ch.njol.skript.classes.ClassInfo
import com.github.tanokun.addon.clazz.definition.Identifier
import com.github.tanokun.addon.instance.InstanceProperty

data class FieldDefinition(
    val fieldName: Identifier,
    val typeName: Identifier,
    val isMutable: Boolean,
    val isArray: Boolean,
    val modifier: Int
) {
    override fun toString(): String {
        val mutability = if (isMutable) "var" else "val"
        val typeStr = if (isArray) "array of $typeName" else typeName
        return "Field(${modifier} $mutability $fieldName: $typeStr)"
    }

    fun newInstance(parentClassName: String, value: Any): InstanceProperty {
/*        if (isArray) {
            if (value !is Array<*>) throw IllegalArgumentException("リスト型である必要があります。")
            return InstanceProperty(fieldName.identifier, parentClassName, true, value)
        }

        if (type.c.isAssignableFrom(value::class.java)) {
            return InstanceProperty(fieldName.identifier, parentClassName, false, value)
        }*/

      //  throw IllegalArgumentException("型が一致しません: expected=${type.c}, actual=${value::class.java.simpleName}")
        TODO()
    }
}
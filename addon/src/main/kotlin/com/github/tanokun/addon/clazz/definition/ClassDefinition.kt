package com.github.tanokun.addon.clazz.definition

import ch.njol.skript.classes.ClassInfo
import ch.njol.skript.effects.Delay
import com.github.tanokun.addon.clazz.definition.field.FieldDefinition
import com.github.tanokun.addon.clazz.definition.function.FunctionDefinition
import com.github.tanokun.addon.instance.AnyInstance
import net.bytebuddy.utility.JavaConstant
import java.lang.invoke.MethodHandles

data class ClassDefinition(
    val className: Identifier,
    val fields: List<FieldDefinition>,
    val functions: List<FunctionDefinition>,
) {

    fun newInstance(arguments: Collection<Any>): AnyInstance {
        TODO()
/*        if (fields.size != arguments.size) {
            throw IllegalArgumentException("引数の数が一致しません: expected=${fields.size}, actual=${arguments.size}")
        }

        return classInfo.c.newInstance().apply {
            this.className = this@ClassDefinition.className.identifier
            this.properties = arguments.mapIndexed { index, value -> fields[index].newInstance(className, value) }
        }*/
    }

    fun getFunction(funcName: Identifier) = functions.firstOrNull { it.name == funcName }
}
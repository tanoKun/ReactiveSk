package com.github.tanokun.addon.clazz

import ch.njol.skript.classes.Changer
import ch.njol.skript.lang.Expression
import ch.njol.skript.registrations.Classes
import com.github.tanokun.addon.clazz.definition.ClassDefinition
import com.github.tanokun.addon.clazz.definition.Identifier
import com.github.tanokun.addon.instance.AnyInstance
import org.bukkit.event.Event
import kotlin.jvm.java

object ClassRegistry {
    private val classes = mutableMapOf<Identifier, ClassDefinition>()

    private val cache = hashMapOf<String, ClassDefinition>()

    fun registerClass(name: Identifier, definition: ClassDefinition) {
        classes[name] = definition
        cache[name.identifier] = definition
    }

    fun newInstance(className: Identifier, expressions: Array<Expression<Any>>, e: Event): AnyInstance {
        val classDefinition = classes[className] ?: throw IllegalArgumentException("Class $className is not registered.")

        val fields = classDefinition.fields

        if (fields.size != expressions.size) {
            throw IllegalArgumentException("引数の数が一致しません: expected=${fields.size}, actual=${expressions.size}")
        }

        val arguments = expressions.mapIndexed { index, expression ->
            val field = fields[index]
            val type = Classes.getClass(field.typeName.lowercase())

            val casted = expression.getConvertedExpression(type)

            val value =
                if (field.isArray) {
                    val array = casted?.getAll(e) ?: throw IllegalArgumentException("型が間違っています。expected=${field.typeName}, actual=${expression.returnType.typeName}")
                    array.forEach {
                        if (!type.isAssignableFrom(it::class.java)) {
                            throw IllegalArgumentException("型が間違っています。expected=${field.typeName}, actual=${it::class.simpleName}")
                        }
                    }

                    array
                } else {
                    val value = casted?.getSingle(e) ?: throw IllegalArgumentException("型が間違っています。expected=${field.typeName}, actual=${expression.returnType.typeName}")
                    if (!type.isAssignableFrom(value::class.java)) {
                        throw IllegalArgumentException("型が間違っています。expected=${field.typeName}, actual=${value::class.simpleName}")
                    }

                    value
                }

            return@mapIndexed value
        }

        return classDefinition.newInstance(arguments)
    }

    fun getClassDefinition(name: String): ClassDefinition? = cache[name]
}
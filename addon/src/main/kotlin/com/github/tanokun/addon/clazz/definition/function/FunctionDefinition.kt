package com.github.tanokun.addon.clazz.definition.function

import ch.njol.skript.classes.Changer
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.Trigger
import ch.njol.skript.lang.TriggerItem
import ch.njol.skript.registrations.Classes
import ch.njol.skript.variables.Variables
import com.github.tanokun.addon.clazz.definition.Identifier
import com.github.tanokun.addon.instance.AnyInstance
import com.github.tanokun.addon.maker.function.FunctionRuntimeBukkitEvent
import org.antlr.v4.Tool
import org.bukkit.event.Event
import kotlin.jvm.java

data class FunctionDefinition(
    val name: Identifier,
    val parameters: List<ParameterDefinition>,
    val returnTypeName: Identifier?,
    val rawBlock: String,
    val modifier: Int
) {
    override fun toString(): String {
        val params = parameters.joinToString(", ") { "${it.name}: ${it.typeName}" }
        return "Function(${modifier} fun $name($params): $returnTypeName { $rawBlock })"
    }

    fun arguments(args: Array<Expression<Any>>, e: Event): List<Pair<String, Any>> {
        TODO()
/*        if (parameters.size != args.size) {
            throw IllegalArgumentException("引数の数が一致しません: expected=${parameters.size}, actual=${args.size}")
        }

        return args.mapIndexed { index, expression ->
            val param = parameters[index]
            val type = Classes.getClass(param.type.c.simpleName.lowercase())

            val casted = expression.getConvertedExpression(type)

            val value =
                if (param.isArray) {
                    val array = casted?.getAll(e) ?: throw IllegalArgumentException("型が間違っています。expected=${param.type.c.simpleName}, actual=${expression.returnType}")
                    array.forEach {
                        if (!type.isAssignableFrom(it::class.java)) {
                            throw IllegalArgumentException("型が間違っています。expected=${param.type.c.simpleName}, actual=${it::class.simpleName}")
                        }
                    }

                    array
                } else {
                    val value = casted?.getSingle(e) ?: throw IllegalArgumentException("型が間違っています。expected=${param.type.c.simpleName}, actual=${expression.returnType}")
                    if (!type.isAssignableFrom(value::class.java)) {
                        throw IllegalArgumentException("型が間違っています。expected=${param.type.c.simpleName}, actual=${value::class.simpleName}")
                    }

                    value
                }

            return@mapIndexed param.name to value
        }*/
    }

    suspend fun call(instance: AnyInstance, arguments: List<Pair<String, Any>>, e: FunctionRuntimeBukkitEvent): Any? {
        TODO()
/*        instance.properties.forEach { property ->
            if (property.isArray) {
                Variables.setVariable("${property.propertyName}::*", property.value, e, true)
                return@forEach
            }

            Variables.setVariable(property.propertyName, property.value, e, true)
        }

        arguments.forEach { (name, value) ->
            Variables.setVariable(name, value, e, true)
        }

        Variables.setVariable("this", instance, e, true)

        TriggerItem.walk(trigger, e)

        return e.getReturn()*/
    }
}

/*
 ```
 //そのクラス事の Observer
observe Person:
    stop if event-param is not age

    # do things
```

```
command /test:
    trigger:
        //インスタンス事のObserver
        join observe {player}:
            # do things
```
 */
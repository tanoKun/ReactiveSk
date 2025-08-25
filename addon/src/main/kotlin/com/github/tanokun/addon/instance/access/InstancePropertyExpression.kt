package com.github.tanokun.addon.instance.access

import ch.njol.skript.Skript
import ch.njol.skript.classes.Changer
import ch.njol.skript.expressions.base.PropertyExpression
import ch.njol.skript.expressions.base.SimplePropertyExpression
import ch.njol.skript.lang.*
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.skript.timings.SkriptTimings
import ch.njol.skript.util.LiteralUtils
import ch.njol.skript.variables.Variables
import ch.njol.util.Kleenean
import ch.njol.util.coll.CollectionUtils.array
import com.github.tanokun.addon.clazz.ClassRegistry
import com.github.tanokun.addon.clazz.definition.Identifier
import com.github.tanokun.addon.coroutineScope
import com.github.tanokun.addon.instance.AnyInstance
import com.github.tanokun.addon.instance.InstanceProperty
import com.github.tanokun.addon.maker.function.AwaitFunctionRuntimeBukkitEvent
import com.github.tanokun.addon.maker.function.NonSuspendFunctionRuntimeBukkitEvent
import com.ibm.icu.text.PluralRules
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bukkit.event.Event
import kotlin.jvm.java

@Suppress("UNCHECKED_CAST")
class InstancePropertyExpression: SimplePropertyExpression<Any, Any>() {

    companion object {
        init {
            Skript.registerExpression(
                InstancePropertyExpression::class.java, Any::class.java, ExpressionType.PROPERTY,
                "%object%.%identifier%[.]"
            )
        }
    }

    private lateinit var propertyName: String

    override fun init(
        exprs: Array<out Expression<*>>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult,
    ): Boolean {
        expr = exprs[0]
        propertyName = (exprs[1] as Expression<Identifier>).getSingle(null)?.identifier ?: let {
            Skript.error("property が存在しません。")
            return false
        }

        return true
    }

    override fun getPropertyName(): String? = ""

    override fun convert(target: Any): Any? {
        when (target) {
            is AnyInstance -> return getProperty(target)
            is InstanceProperty -> {
                val target = target.value as? AnyInstance ?: return null
                return getProperty(target)
            }
        }

        return null
    }

    override fun toString(e: Event?, debug: Boolean): String = "instance property"

    override fun getReturnType(): Class<out Any> = Any::class.java

    override fun acceptChange(mode: Changer.ChangeMode): Array<Class<*>>? = null

    override fun beforeChange(changed: Expression<*>?, delta: Array<out Any?>?): Array<out Any?>? {
        return super.beforeChange(changed, delta)
    }

    fun getProperty(instance: AnyInstance): Any {
        return instance.properties.firstOrNull { it.propertyName == propertyName }
            ?: throw IllegalArgumentException("property $propertyName is not defined in class ${instance.className}.")
    }
}

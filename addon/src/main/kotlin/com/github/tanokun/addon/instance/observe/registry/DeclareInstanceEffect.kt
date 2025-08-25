package com.github.tanokun.addon.instance.observe.registry

import ch.njol.skript.Skript
import ch.njol.skript.classes.Changer
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionList
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.TriggerItem
import ch.njol.skript.lang.Variable
import ch.njol.skript.timings.SkriptTimings
import ch.njol.skript.util.LiteralUtils
import ch.njol.skript.variables.Variables
import ch.njol.util.Kleenean
import com.github.tanokun.addon.clazz.definition.Identifier
import com.github.tanokun.addon.coroutineScope
import com.github.tanokun.addon.instance.AnyInstance
import com.github.tanokun.addon.instance.InstanceProperty
import com.github.tanokun.addon.instance.call.AwaitCallFunctionEffect
import com.github.tanokun.addon.instance.call.call
import com.github.tanokun.addon.instance.call.getInstance
import com.github.tanokun.addon.instance.observe.InstanceObserver
import com.github.tanokun.addon.maker.function.AwaitFunctionRuntimeBukkitEvent
import com.github.tanokun.addon.maker.observe.ObserveInstanceBukkitEvent
import com.sun.jmx.mbeanserver.DefaultMXBeanMappingFactory.propertyName
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.event.Event

@Suppress("UNCHECKED_CAST")
class DeclareInstanceEffect : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                DeclareInstanceEffect::class.java,
                "declare %object%"
            )
        }
    }

    private lateinit var targetExpr: Expression<Any>

    override fun init(
        exprs: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult,
    ): Boolean {
        targetExpr = exprs[0] as Expression<Any>

        return true
    }

    override fun execute(e: Event) {
        val instance = getInstance(targetExpr.getSingle(e) ?: return) ?: return
    }

    override fun toString(e: Event?, debug: Boolean): String? = "declare instance"
}
package com.github.tanokun.addon.runtime.skript.observe

import ch.njol.skript.Skript
import ch.njol.skript.config.Node
import ch.njol.skript.config.SectionNode
import ch.njol.skript.lang.Literal
import ch.njol.skript.lang.SelfRegisteringSkriptEvent
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.Trigger
import com.github.tanokun.addon.definition.DynamicClassInfo
import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.definition.variable.TypedVariableDeclaration
import com.github.tanokun.addon.definition.variable.TypedVariableResolver
import com.github.tanokun.addon.definition.variable.getDepth
import com.github.tanokun.addon.definition.variable.getTopNode
import com.github.tanokun.addon.intermediate.generator.internalFieldOf
import com.github.tanokun.addon.runtime.skript.observe.mediator.RuntimeObservingMediator
import com.github.tanokun.addon.runtime.variable.AmbiguousVariableFrames
import org.bukkit.event.Event

class ObserverSkriptEvent: SelfRegisteringSkriptEvent() {

    companion object {
        init {
            Skript.registerEvent("Observe", ObserverSkriptEvent::class.java, RuntimeObservingMediator::class.java,
                "observe %dynamicclassinfo% factor %identifier%"
            )
        }
    }

    lateinit var factor: String
        private set

    lateinit var dynamicClassInfo: DynamicClassInfo

    private lateinit var topNode: Node

    private lateinit var trigger: Trigger

    override fun init(args: Array<out Literal<*>>, matchedPattern: Int, parseResult: SkriptParser.ParseResult): Boolean {
        dynamicClassInfo = args[0].getSingle(null) as DynamicClassInfo
        factor = (args[1].getSingle(null) as Identifier).identifier

        val field = dynamicClassInfo.clazz.declaredFields.firstOrNull { it.name == internalFieldOf(factor) } ?: let {
            Skript.error("Cannot find field '$factor' in '${dynamicClassInfo.clazz.simpleName}'.")
            return false
        }

        val depth = (parser.node as SectionNode).getDepth()
        topNode = (parser.node as SectionNode).getTopNode()

        TypedVariableResolver.declare(topNode, TypedVariableDeclaration(Identifier("instance"), dynamicClassInfo.clazz, false, depth))
        TypedVariableResolver.declare(topNode, TypedVariableDeclaration(Identifier("old"), field.type, false, depth))
        TypedVariableResolver.declare(topNode, TypedVariableDeclaration(Identifier("new"), field.type, false, depth))

        return true
    }

    override fun register(t: Trigger) {
        trigger = t

        RuntimeObservingMediator.register(this)
    }

    override fun unregister(t: Trigger) {
        RuntimeObservingMediator.unregister(this)
    }

    override fun unregisterAll() {
        RuntimeObservingMediator.unregisterAll()
    }

    fun execute(e: Event, any: Any, old: Any?, new: Any?) {
        try {
            AmbiguousVariableFrames.beginFrame(e)

            AmbiguousVariableFrames.set(e, 0, any)
            AmbiguousVariableFrames.set(e, 1, old)
            AmbiguousVariableFrames.set(e, 2, new)

            trigger.execute(e)
        } catch (ex: Throwable) {
            ex.printStackTrace()
        } finally {
            AmbiguousVariableFrames.endFrame(e)
        }
    }

    override fun toString(e: Event?, debug: Boolean): String = hashCode().toString()
}
package com.github.tanokun.reactivesk.v263.skript.runtime.observe

import ch.njol.skript.Skript
import ch.njol.skript.config.Node
import ch.njol.skript.config.SectionNode
import ch.njol.skript.lang.Literal
import ch.njol.skript.lang.SelfRegisteringSkriptEvent
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.Trigger
import com.github.tanokun.reactivesk.compiler.backend.codegen.util.internalFieldOf
import com.github.tanokun.reactivesk.compiler.frontend.analyze.variable.TypedVariableDeclaration
import com.github.tanokun.reactivesk.lang.Identifier
import com.github.tanokun.reactivesk.v263.AmbiguousVariableFrames
import com.github.tanokun.reactivesk.v263.ReactiveSkAddon
import com.github.tanokun.reactivesk.v263.skript.DynamicClassInfo
import com.github.tanokun.reactivesk.v263.skript.runtime.observe.mediator.RuntimeObservingMediator
import com.github.tanokun.reactivesk.v263.skript.util.getDepth
import com.github.tanokun.reactivesk.v263.skript.util.getTopNode
import org.bukkit.event.Event

class ObserverSkriptEvent: SelfRegisteringSkriptEvent() {

    companion object {
        fun register() {
            Skript.registerEvent("Observe", ObserverSkriptEvent::class.java, RuntimeObservingMediator::class.java,
                "observe %dynamicclassinfo% factor %identifier%"
            )
        }
    }

    private val typedVariableResolver = ReactiveSkAddon.typedVariableResolver

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

        typedVariableResolver.declare(topNode,
            TypedVariableDeclaration.Unresolved(Identifier("instance"), dynamicClassInfo.clazz, false, depth)
        )
        typedVariableResolver.declare(topNode,
            TypedVariableDeclaration.Unresolved(Identifier("old"), field.type, false, depth)
        )
        typedVariableResolver.declare(topNode,
            TypedVariableDeclaration.Unresolved(Identifier("new"), field.type, false, depth)
        )

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

    fun execute(e: Event, instance: Any, old: Any?, new: Any?) {
        try {
            AmbiguousVariableFrames.set(e, 0, instance)
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
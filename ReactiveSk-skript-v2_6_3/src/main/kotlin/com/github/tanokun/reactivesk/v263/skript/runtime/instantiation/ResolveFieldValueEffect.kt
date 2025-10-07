package com.github.tanokun.reactivesk.v263.skript.runtime.instantiation

import ch.njol.skript.Skript
import ch.njol.skript.config.Node
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.TriggerItem
import ch.njol.skript.sections.SecLoop
import ch.njol.skript.sections.SecWhile
import ch.njol.skript.util.LiteralUtils
import ch.njol.util.Kleenean
import com.github.tanokun.reactivesk.compiler.backend.codegen.util.internalArrayListSetterOf
import com.github.tanokun.reactivesk.compiler.backend.codegen.util.internalFieldOf
import com.github.tanokun.reactivesk.compiler.backend.codegen.util.internalSetterOf
import com.github.tanokun.reactivesk.lang.Identifier
import com.github.tanokun.reactivesk.v263.AmbiguousVariableFrames
import com.github.tanokun.reactivesk.v263.ReactiveSkAddon
import com.github.tanokun.reactivesk.v263.skript.resolve.clazz.ConstructorInjectorSection
import com.github.tanokun.reactivesk.v263.skript.runtime.instantiation.mediator.RuntimeConstructorMediator
import com.github.tanokun.reactivesk.v263.skript.util.getDepth
import com.github.tanokun.reactivesk.v263.skript.util.getTopNode
import org.bukkit.event.Event
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles

@Suppress("UNCHECKED_CAST")
class ResolveFieldValueEffect: Effect() {
    private val lookup = MethodHandles.publicLookup()

    companion object {
        fun register() {
            Skript.registerEffect(
                ResolveFieldValueEffect::class.java,
                "resolve %identifier% \\:= %object%"
            )
        }
    }

    private val typedVariableResolver = ReactiveSkAddon.typedVariableResolver

    lateinit var fieldName: Identifier
        private set

    lateinit var parseNode: Node
        private set

    private lateinit var valueExpr: Expression<Any>

    private lateinit var setterHandle: MethodHandle

    private lateinit var getterHandle: MethodHandle

    override fun init(
        exprs: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult,
    ): Boolean {

        val injector = parser.currentSections
            .filterIsInstance<ConstructorInjectorSection>()
            .firstOrNull()
            ?: let {
                Skript.error("Cannot resolve typed value field because it's not constructor section.")
                return false
            }

        fieldName = (exprs[0] as Expression<Identifier>).getSingle(null) ?: let {
            Skript.error("Field name is not specified. ${exprs[1]}")
            return false
        }

        parseNode = parser.node ?: return false
        val internalFieldName = internalFieldOf(fieldName.identifier)
        val clazz = injector.resolvingClass.clazz

        val targetFields = injector.resolvingClass.definition.uninitializedProperty.map { it.propertyName }

        if (!targetFields.contains(fieldName)) {
            Skript.error("This field '$fieldName' is not required to be initialized in '${clazz.simpleName}'.")
            return false
        }

        val field = clazz.declaredFields.firstOrNull { it.name == internalFieldName } ?: let {
            Skript.error("Cannot find field '$fieldName' in '${clazz.simpleName}'.")
            return false
        }

        valueExpr = LiteralUtils.defendExpression(exprs[1])

        if (!valueExpr.isSingle) {
            Skript.error("Definition '$valueExpr' must be single.")
            return false
        }

        if (!field.type.isAssignableFrom(valueExpr.returnType)) {
            Skript.error("Cannot assign '$valueExpr' to field '$fieldName' because it's not type '${field.type.simpleName}' but '${valueExpr.returnType.simpleName}'")
            return false
        }

        if (parser.currentSections.any { it is SecLoop || it is SecWhile }) {
            Skript.warning("Resolving field '$fieldName' inside a loop may cause 'already initialized' error if the field is already initialized.")
        }


        val setterName = if (field.type == ArrayList::class.java)
            internalArrayListSetterOf(fieldName.identifier)
        else
            internalSetterOf(fieldName.identifier)

        val setterMethod = clazz.methods.first { it.name == setterName }

        setterHandle = lookup.unreflect(setterMethod)
        getterHandle = lookup.unreflectGetter(field)

        val depth = parseNode.getDepth()
        val topNode = parseNode.getTopNode()

        typedVariableResolver.touchSection(topNode, depth, parser.currentSections.lastOrNull())

        typedVariableResolver.getDeclarationInScopeChain(topNode, depth, Identifier("this")) ?: let {
            Skript.error("Cannot find 'this' variable in scope chain.")
            return false
        }

        return true
    }

    override fun execute(e: Event?) =
        throw UnsupportedOperationException("Cannot execute ResolveFieldValueEffect directly. It must be executed in walk.")


    override fun walk(e: Event): TriggerItem? {
        e as RuntimeConstructorMediator

        val target = AmbiguousVariableFrames.get(e, 0) ?: throw IllegalStateException("Integrity of 'this' is broken.")
        val value = valueExpr.getSingle(e) ?: throw IllegalStateException("Integrity of '$valueExpr' is broken.")

        if (getterHandle.invoke(target) != null) {
            Skript.error("Cannot resolve field '$fieldName' in '${target::class.java.simpleName}' because it's already initialized.\"")
            return null
        }

        setterHandle.invoke(target, value, false)

        return next
    }

    override fun toString(e: Event?, debug: Boolean): String = "resolve $fieldName \\:= $valueExpr"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ResolveFieldValueEffect) return false

        if (fieldName != other.fieldName) return false

        return true
    }

    override fun hashCode(): Int {
        return fieldName.hashCode()
    }
}
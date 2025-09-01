package com.github.tanokun.addon.runtime.skript.init

import ch.njol.skript.Skript
import ch.njol.skript.config.Node
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.sections.SecLoop
import ch.njol.skript.sections.SecWhile
import ch.njol.skript.util.LiteralUtils
import ch.njol.util.Kleenean
import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.definition.skript.dynamic.InitDefinitionInjector
import com.github.tanokun.addon.definition.variable.TypedVariableResolver
import com.github.tanokun.addon.definition.variable.getDepth
import com.github.tanokun.addon.definition.variable.getTopNode
import com.github.tanokun.addon.intermediate.generator.fieldOf
import com.github.tanokun.addon.intermediate.generator.internalArrayListSetterOf
import com.github.tanokun.addon.intermediate.generator.internalSetterOf
import com.github.tanokun.addon.runtime.skript.init.mediator.RuntimeConstructorMediator
import com.github.tanokun.addon.runtime.variable.AmbiguousVariableFrames
import org.bukkit.event.Event
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

@Suppress("UNCHECKED_CAST")
class ResolveTypedValueFieldEffect: Effect() {
    private val lookup = MethodHandles.publicLookup()

    companion object {
        init {
            Skript.registerEffect(ResolveTypedValueFieldEffect::class.java,
                "resolve %*identifier% \\:= %object%"
            )
        }
    }

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

        val injector = parser.currentSections.firstOrNull { it is InitDefinitionInjector } as? InitDefinitionInjector ?: let {
            Skript.error("Cannot resolve typed value field because it's not init section.")
            return false
        }

        fieldName = (exprs[0] as Expression<Identifier>).getSingle(null) ?: let {
            Skript.error("Field name is not specified. ${exprs[1]}")
            return false
        }

        parseNode = parser.node ?: return false
        val internalFieldName = fieldOf(fieldName.identifier)
        val clazz = injector.thisDynamicClass

        val targetFields = injector.thisClassDefinition.getRequiredInitializationFields().map { it.fieldName }

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

        setterHandle =
            if (field.type == ArrayList::class.java) {
                lookup.findVirtual(
                    clazz,
                    internalArrayListSetterOf(fieldName.identifier),
                    MethodType.methodType(Void.TYPE, ArrayList::class.java)
                )
            } else
                lookup.findVirtual(
                    clazz,
                    internalSetterOf(fieldName.identifier),
                    MethodType.methodType(Void.TYPE, valueExpr.returnType)
                )


        getterHandle = lookup.unreflectGetter(field)

        val depth = parseNode.getDepth()
        val topNode = parseNode.getTopNode()

        TypedVariableResolver.touchSection(topNode, depth, parser.currentSections.lastOrNull())

        TypedVariableResolver.getDeclarationInScopeChain(topNode, depth, Identifier("this")) ?: let {
            Skript.error("Cannot find 'this' variable in scope chain.")
            return false
        }

        return true
    }

    override fun execute(e: Event) {
        e as RuntimeConstructorMediator

        val target = AmbiguousVariableFrames.get(e, 0) ?: throw IllegalStateException("Integrity of 'this' is broken.")
        val value = valueExpr.getSingle(e) ?: throw IllegalStateException("Integrity of '$valueExpr' is broken.")

        if (getterHandle.invoke(target) != null) throw IllegalStateException("Cannot resolve field '$fieldName' in '${target::class.java.simpleName}' because it's already initialized.")
        setterHandle.invoke(target, value)
    }

    override fun toString(e: Event?, debug: Boolean): String = "resolve $fieldName \\:= $valueExpr"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ResolveTypedValueFieldEffect) return false

        if (fieldName != other.fieldName) return false

        return true
    }

    override fun hashCode(): Int {
        return fieldName.hashCode()
    }
}
package com.github.tanokun.addon.runtime.skript.field

import ch.njol.skript.Skript
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.util.LiteralUtils
import ch.njol.util.Kleenean
import com.github.tanokun.addon.definition.dynamic.PropertyModifiers.isImmutable
import com.github.tanokun.addon.intermediate.generator.internalArrayListSetterOf
import com.github.tanokun.addon.intermediate.generator.internalSetterOf
import com.github.tanokun.addon.intermediate.metadata.ModifierMetadata
import com.github.tanokun.addon.runtime.DynamicAccessChecks.checkAccessError
import com.github.tanokun.addon.runtime.skript.SkriptExpressionInitChecks.checkSingletonError
import org.bukkit.event.Event
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Field

@Suppress("UNCHECKED_CAST")
class SetTypedValueFieldEffect: Effect() {
    private val lookup = MethodHandles.publicLookup()

    companion object {
        init {
            Skript.registerEffect(SetTypedValueFieldEffect::class.java,
                "%object% \\<- %object%",
            )
        }
    }

    private lateinit var field: Field

    private lateinit var targetExpr: Expression<Any>

    private lateinit var valueExpr: Expression<Any>

    private lateinit var setterHandle: MethodHandle

    override fun init(
        exprs: Array<out Expression<*>>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult,
    ): Boolean {
        val fieldExpr = (exprs[0] as? GetTypedValueFieldExpression) ?: let {
            Skript.error("Cannot set typed value field ${exprs.getOrNull(0)} because it's not field.")
            return false
        }

        field = fieldExpr.field
        targetExpr = fieldExpr.targetExpr

        val modifiers = field.getAnnotation(ModifierMetadata::class.java).modifiers

        if (fieldExpr.targetClass.checkAccessError(parser, modifiers, "Cannot write field '${fieldExpr.fieldName}' in '${fieldExpr.targetClass.simpleName}' because it is private field.")) return false

        if (modifiers.isImmutable()) {
            Skript.error("Cannot write field '${fieldExpr.fieldName}' in '${fieldExpr.targetClass.simpleName}' because it is immutable field.")
        }

        if (checkSingletonError(exprs[1])) return false
        valueExpr = LiteralUtils.defendExpression(exprs[1])

        if (!field.type.isAssignableFrom(valueExpr.returnType)) {
            Skript.error("Cannot assign $valueExpr to field '${fieldExpr.fieldName}' because it's not type '${field.type.simpleName}' but '${valueExpr.returnType.simpleName}'")
            return false
        }

        setterHandle =
            if (field.type == ArrayList::class.java) {
                lookup.findVirtual(
                    targetExpr.getReturnType(),
                    internalArrayListSetterOf(fieldExpr.fieldName.identifier),
                    MethodType.methodType(Void.TYPE, ArrayList::class.java)
                )
            } else lookup.findVirtual(
                targetExpr.getReturnType(),
                internalSetterOf(fieldExpr.fieldName.identifier),
                MethodType.methodType(Void.TYPE, valueExpr.returnType)
            )

        return true
    }

    override fun execute(e: Event) {
        val target = targetExpr.getSingle(e) ?: throw IllegalStateException("Integrity of '$targetExpr' is broken.")
        val value = valueExpr.getSingle(e) ?: throw IllegalStateException("Integrity of '$valueExpr' is broken.")

        setterHandle.invoke(target, value)
    }

    override fun toString(e: Event?, debug: Boolean): String = ""

}
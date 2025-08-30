package com.github.tanokun.addon.runtime.skript.field

import ch.njol.skript.Skript
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.util.LiteralUtils
import ch.njol.util.Kleenean
import com.github.tanokun.addon.intermediate.generator.internalArrayListSetterOf
import com.github.tanokun.addon.intermediate.metadata.ModifierMetadata
import com.github.tanokun.addon.intermediate.metadata.MutableFieldMetadata
import com.github.tanokun.addon.lookup
import org.bukkit.event.Event
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodType
import java.lang.reflect.Field
import java.lang.reflect.Modifier

@Suppress("UNCHECKED_CAST")
class SetTypedValueFieldEffect: Effect() {
    companion object {
        init {
            Skript.registerEffect(SetTypedValueFieldEffect::class.java,
                "%object% -> %object%",
            )
        }
    }

    private lateinit var field: Field

    private lateinit var targetExpr: Expression<Any>

    private lateinit var valueExpr: Expression<Any>

    private lateinit var setterHandle: MethodHandle

    override fun init(
        exprs: Array<out Expression<*>?>,
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

        if (Modifier.isPrivate(field.getAnnotation(ModifierMetadata::class.java).modifiers) && !fieldExpr.isInThisClass(parser, fieldExpr.targetClass)) {
            Skript.error("Cannot write field '${fieldExpr.fieldName}' in '${fieldExpr.targetClass.simpleName}' because it is private field.")
            return false
        }

        if (!field.isAnnotationPresent(MutableFieldMetadata::class.java)) {
            Skript.error("Cannot write field '${fieldExpr.fieldName}' in '${fieldExpr.targetClass.simpleName}' because it is immutable field.")
        }

        valueExpr = LiteralUtils.defendExpression(exprs[1])

        if (!valueExpr.isSingle) {
            Skript.error("Definition '$valueExpr' must be single.")
            return false
        }

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
            } else lookup.unreflectSetter(field)


        return true
    }

    override fun execute(e: Event) {
        val target = targetExpr.getSingle(e) ?: throw IllegalStateException("Integrity of '$targetExpr' is broken.")
        val value = valueExpr.getSingle(e) ?: throw IllegalStateException("Integrity of '$valueExpr' is broken.")

        setterHandle.invoke(target, value)
    }

    override fun toString(e: Event?, debug: Boolean): String = ""

}
package com.github.tanokun.reactivesk.v263.skript.runtime.variable.field

import ch.njol.skript.Skript
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.util.LiteralUtils
import ch.njol.util.Kleenean
import com.github.tanokun.reactivesk.compiler.backend.codegen.util.internalArrayListSetterOf
import com.github.tanokun.reactivesk.compiler.backend.codegen.util.internalSetterOf
import com.github.tanokun.reactivesk.compiler.backend.metadata.ModifierMetadata
import com.github.tanokun.reactivesk.lang.PropertyModifiers.isImmutable
import com.github.tanokun.reactivesk.v263.skript.runtime.DynamicAccessChecks.checkAccessError
import com.github.tanokun.reactivesk.v263.skript.runtime.SkriptExpressionInitChecks.checkSingletonError
import com.github.tanokun.reactivesk.v263.skript.util.PriorityRegistration
import org.bukkit.event.Event
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Field

@Suppress("UNCHECKED_CAST")
class SetTypedValueFieldEffect: Effect() {
    private val lookup = MethodHandles.publicLookup()

    companion object {
        fun register() {
            PriorityRegistration.register<SetTypedValueFieldEffect>(
                "set %object% to %object%",
                "notify (that | ->) set %object% to %object%",
            )
        }
    }

    private lateinit var field: Field

    private lateinit var targetExpr: Expression<Any>

    private lateinit var valueExpr: Expression<Any>

    private lateinit var setterHandle: MethodHandle

    private var shouldNotify: Boolean = false

    override fun init(
        exprs: Array<out Expression<*>>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult,
    ): Boolean {
        val fieldExpr = (exprs[0] as? GetTypedValueFieldExpression) ?: let {
            return false
        }

        shouldNotify = matchedPattern == 1
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
                    MethodType.methodType(Void.TYPE, ArrayList::class.java, Boolean::class.java)
                )
            } else lookup.findVirtual(
                targetExpr.getReturnType(),
                internalSetterOf(fieldExpr.fieldName.identifier),
                MethodType.methodType(Void.TYPE, valueExpr.returnType, Boolean::class.java)
            )

        return true
    }

    override fun execute(e: Event) {
        val target = targetExpr.getSingle(e) ?: throw IllegalStateException("Integrity of '$targetExpr' is broken.")
        val value = valueExpr.getSingle(e) ?: throw IllegalStateException("Integrity of '$valueExpr' is broken.")

        setterHandle.invoke(target, value, shouldNotify)
    }

    override fun toString(e: Event?, debug: Boolean): String = ""

}
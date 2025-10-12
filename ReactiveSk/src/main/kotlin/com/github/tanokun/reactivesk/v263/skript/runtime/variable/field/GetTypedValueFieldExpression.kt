package com.github.tanokun.reactivesk.v263.skript.runtime.variable.field

import ch.njol.skript.Skript
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionType
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.util.Kleenean
import com.github.tanokun.reactivesk.compiler.backend.codegen.util.internalFieldOf
import com.github.tanokun.reactivesk.compiler.backend.metadata.ModifierMetadata
import com.github.tanokun.reactivesk.lang.Identifier
import com.github.tanokun.reactivesk.v263.skript.DynamicClass
import com.github.tanokun.reactivesk.v263.skript.runtime.DynamicAccessChecks.checkAccessError
import org.bukkit.event.Event
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.Field

@Suppress("UNCHECKED_CAST")
class GetTypedValueFieldExpression: SimpleExpression<Any>() {
    private val lookup = MethodHandles.publicLookup()

    companion object {
        fun register() {
            Skript.registerExpression(
                GetTypedValueFieldExpression::class.java, Any::class.java, ExpressionType.PROPERTY,
                "%object%(->|\\.)%identifier%",
                "%identifier% of %object%"
            )
        }
    }

    lateinit var field: Field
        private set

    lateinit var fieldName: Identifier
        private set

    lateinit var targetExpr: Expression<Any>
        private set

    val targetClass: Class<*> get() = targetExpr.getReturnType()

    private var returnType: Class<out Any> = Void.TYPE

    private var isArray: Boolean = false

    private lateinit var getterHandle: MethodHandle

    override fun init(
        exprs: Array<out Expression<*>>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult,
    ): Boolean {
        val targetExprPattern = if (matchedPattern == 0) 0 else 1
        val fieldNamePattern = if (matchedPattern == 0) 1 else 0

        targetExpr = exprs[targetExprPattern] as Expression<Any>

        fieldName = (exprs[fieldNamePattern] as Expression<Identifier>).getSingle(null) ?: let {
            Skript.error("Field name is not specified. ${exprs[1]}")
            return false
        }

        val internalFieldName = internalFieldOf(fieldName.identifier)

        val clazz = targetExpr.getReturnType()

        if (!DynamicClass::class.java.isAssignableFrom(clazz)) {
            Skript.error("Cannot read field '$fieldName' in '${clazz.simpleName}' because it is not dynamic class.")
            return false
        }

        field = clazz.declaredFields.firstOrNull { it.name == internalFieldName } ?: let {
            Skript.error("Cannot find field '$fieldName' in '${clazz.simpleName}'.")
            return false
        }

        val modifiers = field.getAnnotation(ModifierMetadata::class.java).modifiers

        if (clazz.checkAccessError(parser, modifiers, "Cannot read field '$fieldName' in '${clazz.simpleName}' because it is private field.")) return false

        returnType = field.type
        isArray = returnType == ArrayList::class.java
        getterHandle = lookup.unreflectGetter(field)

        return true
    }

    override fun getReturnType() = returnType

    override fun iterator(e: Event): Iterator<Any>? {
        if (isArray) {
            val list = getSingle(e) as ArrayList<*>
            return list.filterNotNull().iterator()
        }

        return null
    }

    override fun toString(e: Event?, debug: Boolean): String = "$targetExpr.$fieldName"

    override fun get(e: Event): Array<out Any> {
        val target = targetExpr.getSingle(e) ?: throw IllegalStateException("Integrity of '$targetExpr' is broken.")
        val value = getterHandle.invoke(target) ?: throw IllegalStateException("Field '$fieldName' in '${targetExpr.returnType.simpleName}' is not initialized.")

        return arrayOf(value)
    }

    override fun getAll(e: Event): Array<out Any> = get(e)

    override fun isSingle(): Boolean = true
}

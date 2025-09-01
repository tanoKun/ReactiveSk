package com.github.tanokun.addon.runtime.skript.field

import ch.njol.skript.Skript
import ch.njol.skript.classes.Changer
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionType
import ch.njol.skript.lang.SkriptParser
import ch.njol.util.Checker
import ch.njol.util.Kleenean
import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.definition.dynamic.DynamicClass
import com.github.tanokun.addon.intermediate.generator.fieldOf
import com.github.tanokun.addon.intermediate.metadata.ModifierMetadata
import com.github.tanokun.addon.runtime.DynamicAccessChecks.checkAccessError
import org.bukkit.event.Event
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.Field

@Suppress("UNCHECKED_CAST")
class GetTypedValueFieldExpression: Expression<Any> {
    private val lookup = MethodHandles.publicLookup()

    companion object {
        init {
            Skript.registerExpression(
                GetTypedValueFieldExpression::class.java, Any::class.java, ExpressionType.PROPERTY,
                "%object%.%*identifier%[.]"
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
        targetExpr = exprs[0] as Expression<Any>

        fieldName = (exprs[1] as Expression<Identifier>).getSingle(null) ?: let {
            Skript.error("Field name is not specified. ${exprs[1]}")
            return false
        }

        val internalFieldName = fieldOf(fieldName.identifier)

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

    override fun getAnd(): Boolean = true

    override fun setTime(time: Int): Boolean { return false }

    override fun getTime(): Int = 0

    override fun isDefault(): Boolean = false

    override fun iterator(e: Event): Iterator<Any>? {
        if (isArray) {
            val list = getSingle(e) as ArrayList<*>
            return list.filterNotNull().iterator()
        }

        return null
    }

    override fun isLoopOf(s: String): Boolean = false

    override fun getSource(): Expression<*> = this

    override fun simplify(): Expression<out Any> = this

    override fun acceptChange(mode: Changer.ChangeMode?): Array<out Class<*>> = arrayOf()

    override fun change(e: Event, delta: Array<out Any?>, mode: Changer.ChangeMode) { }

    override fun toString(e: Event?, debug: Boolean): String = "$targetExpr.$fieldName"

    override fun getSingle(e: Event): Any {
        val target = targetExpr.getSingle(e) ?: throw IllegalStateException("Integrity of '$targetExpr' is broken.")
        val value = getterHandle.invoke(target) ?: throw IllegalStateException("Field '$fieldName' in '${targetExpr.returnType.simpleName}' is not initialized.")

        return value
    }

    override fun getArray(e: Event): Array<out Any> = arrayOf(getSingle(e))

    override fun getAll(e: Event): Array<out Any> = arrayOf(getSingle(e))

    override fun isSingle(): Boolean = true

    override fun check(e: Event, c: Checker<in Any>, negated: Boolean): Boolean = true

    override fun check(e: Event, c: Checker<in Any>?): Boolean = true

    override fun <R : Any?> getConvertedExpression(vararg to: Class<R>): Expression<out R>? = null
}

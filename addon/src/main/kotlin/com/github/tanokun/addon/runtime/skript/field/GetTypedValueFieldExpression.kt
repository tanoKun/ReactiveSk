package com.github.tanokun.addon.runtime.skript.field

import ch.njol.skript.Skript
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionType
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.parser.ParserInstance
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.util.Kleenean
import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.definition.dynamic.DynamicClass
import com.github.tanokun.addon.definition.skript.dynamic.FunctionDefinitionInjector
import com.github.tanokun.addon.definition.skript.dynamic.InitDefinitionInjector
import com.github.tanokun.addon.intermediate.generator.fieldOf
import com.github.tanokun.addon.intermediate.metadata.ModifierMetadata
import com.github.tanokun.addon.lookup
import org.bukkit.event.Event
import java.lang.invoke.MethodHandle
import java.lang.reflect.Field
import java.lang.reflect.Modifier

@Suppress("UNCHECKED_CAST")
class GetTypedValueFieldExpression: SimpleExpression<Any>() {

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

    private var returnType: Class<out Any>? = Any::class.java

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

        returnType = field.type

        if (Modifier.isPrivate(field.getAnnotation(ModifierMetadata::class.java).modifiers) && !isInThisClass(parser, clazz)) {
            Skript.error("Cannot read field '$fieldName' in '${clazz.simpleName}' because it is private field.")
            return false
        }

        getterHandle = lookup.unreflectGetter(field)

        return true
    }

    fun isInThisClass(parser: ParserInstance, type: Class<*>): Boolean {
        val callerInInit = parser.getCurrentSections(InitDefinitionInjector::class.java).firstOrNull() ?: return false
        if (callerInInit.thisDynamicClass == type) return true

        val callerInFunction = parser.getCurrentSections(FunctionDefinitionInjector::class.java).firstOrNull() ?: return false
        return callerInFunction.thisDynamicClass == type
    }

    override fun get(e: Event): Array<out Any>? {
        val target = targetExpr.getSingle(e) ?: throw IllegalStateException("Integrity of '$targetExpr' is broken.")
        val value = getterHandle.invoke(target) ?: throw IllegalStateException("Field '$fieldName' in '${targetExpr.returnType.simpleName}' is not initialized.")

        return arrayOf(value)
    }

    override fun getReturnType() = returnType

    override fun toString(e: Event?, debug: Boolean): String = "$targetExpr.$fieldName"

    override fun isSingle(): Boolean = true
}

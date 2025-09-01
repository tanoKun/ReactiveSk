package com.github.tanokun.addon.intermediate.generator

import com.github.tanokun.addon.definition.dynamic.ClassDefinition
import com.github.tanokun.addon.intermediate.generator.helper.SetListHelper
import com.github.tanokun.addon.intermediate.integrity.FieldName
import com.github.tanokun.addon.intermediate.integrity.SetAndNotifyValueAdvice
import com.github.tanokun.addon.intermediate.integrity.SetValueAdvice
import com.github.tanokun.addon.intermediate.metadata.ModifierMetadata
import com.github.tanokun.addon.module.ModuleManager
import net.bytebuddy.asm.Advice
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.implementation.FieldAccessor
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.implementation.MethodCall
import net.bytebuddy.implementation.StubMethod
import net.bytebuddy.implementation.bytecode.assign.Assigner
import java.lang.reflect.Method
import java.lang.reflect.Modifier

class FieldsDefiner(
    private val moduleManager: ModuleManager
) {
    fun defineFields(
        builder: DynamicType.Builder<*>,
        classDefinition: ClassDefinition,
    ): DynamicType.Builder<*> {
        var current = builder

        classDefinition.fields.forEach { field ->
            val name = field.fieldName.identifier
            val fieldType = moduleManager.resolveTypeDescription(field.typeName, field.isArray)
            val actualType = moduleManager.resolveTypeDescription(field.typeName, false)

            current = defineBaseField(current, name, fieldType, field.modifiers)

            val advice = buildAdvice(name, actualType)

            current = if (!field.isArray) {
                defineNonArraySetter(current, name, fieldType, field.isFactor(), advice)
            } else {
                defineArraySetters(current, name, actualType, field.isFactor())
            }
        }

        return current
    }

    private fun defineBaseField(
        builder: DynamicType.Builder<*>,
        name: String,
        fieldType: TypeDescription,
        modifiers: Int
    ): DynamicType.Builder<*> {
        return builder
            .defineField(fieldOf(name), fieldType, Modifier.PUBLIC)
            .annotateField(ModifierMetadata(modifiers))
    }

    private fun buildAdvice(
        name: String,
        actualType: TypeDescription
    ): Advice.WithCustomMapping {
        val fieldMapping = Advice.OffsetMapping.ForField.Unresolved.WithImplicitType(
            actualType.asGenericType(),
            false,
            Assigner.Typing.DYNAMIC,
            fieldOf(name),
        )
        return Advice.withCustomMapping()
            .bind(Advice.OffsetMapping.Factory.Simple(Advice.FieldValue::class.java, fieldMapping))
    }

    private fun defineNonArraySetter(
        builder: DynamicType.Builder<*>,
        name: String,
        fieldType: TypeDescription,
        isFactor: Boolean,
        advice: Advice.WithCustomMapping,
    ): DynamicType.Builder<*> {
        val setterName = internalSetterOf(name)

        val adviceProxy = if (isFactor)
            advice.bind(FieldName::class.java, name).to(SetAndNotifyValueAdvice::class.java)
        else
            advice.to(SetValueAdvice::class.java)

        return builder
            .defineMethod(setterName, Void.TYPE, Modifier.PUBLIC)
            .withParameters(fieldType)
            .intercept(adviceProxy.wrap(StubMethod.INSTANCE))
    }

    private fun defineArraySetters(
        builder: DynamicType.Builder<*>,
        name: String,
        actualType: TypeDescription,
        isFactor: Boolean
    ): DynamicType.Builder<*> {
        val setterName = internalArrayListSetterOf(name)

        val checkTypeImpl = buildCheckTypeImpl(name, actualType)
        val notifyImpl = if (isFactor) buildNotifyImpl(name, checkTypeImpl) else checkTypeImpl

        val result = builder
            .defineMethod(setterName, Void.TYPE, Modifier.PUBLIC)
            .withParameters(java.util.ArrayList::class.java)
            .intercept(notifyImpl)
            .let {
                if (isFactor)
                    it.defineMethod(internalArrayListSetterWithoutNotificationOf(name), Void.TYPE, Modifier.PUBLIC)
                        .withParameters(ArrayList::class.java)
                        .intercept(checkTypeImpl)
                else it
            }

        return result
    }

    private fun buildCheckTypeImpl(
        name: String,
        actualType: TypeDescription
    ): Implementation.Composable {
        return MethodCall.invoke(CHECK_TYPES_METHOD)
            .withArgument(0)
            .with(actualType)
            .andThen(FieldAccessor.ofField(fieldOf(name)).setsArgumentAt(0))
    }

    private fun buildNotifyImpl(
        name: String,
        base: Implementation.Composable
    ): Implementation.Composable {
        return base.andThen(
            MethodCall.invoke(NOTIFY_METHOD)
                .withThis()
                .withField(fieldOf(name))
                .withArgument(0)
                .with(name)
        )
    }

    companion object {
        private val CHECK_TYPES_METHOD: Method = SetListHelper::class.java.getMethod(
            "checkTypes",
            java.util.ArrayList::class.java,
            Class::class.java
        )

        private val NOTIFY_METHOD: Method = SetListHelper::class.java.getMethod(
            "notify",
            Any::class.java,
            java.util.ArrayList::class.java,
            java.util.ArrayList::class.java,
            String::class.java
        )
    }
}
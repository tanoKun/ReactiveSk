package com.github.tanokun.reactivesk.compiler.backend.codegen.field

import com.github.tanokun.reactivesk.compiler.backend.ClassResolver
import com.github.tanokun.reactivesk.compiler.backend.codegen.util.internalArrayListSetterOf
import com.github.tanokun.reactivesk.compiler.backend.codegen.util.internalFieldOf
import com.github.tanokun.reactivesk.compiler.backend.codegen.util.internalSetterOf
import com.github.tanokun.reactivesk.compiler.backend.intrinsics.JVM_SETTER_INSTANCE_FIELD
import com.github.tanokun.reactivesk.compiler.backend.intrinsics.JvmSetterIntrinsics
import com.github.tanokun.reactivesk.compiler.backend.intrinsics.getCheckTypesMethod
import com.github.tanokun.reactivesk.compiler.backend.metadata.ModifierMetadata
import com.github.tanokun.reactivesk.lang.ClassDefinition
import com.github.tanokun.reactivesk.lang.PropertyModifiers.isFactor
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.implementation.MethodCall
import java.lang.reflect.Modifier

class FieldsGenerator(
    private val classResolver: ClassResolver,
    private val jvmSetterIntrinsics: Class<out JvmSetterIntrinsics>,
) {
    fun defineFields(
        builder: DynamicType.Builder<*>,
        classDefinition: ClassDefinition,
    ): DynamicType.Builder<*> {
        var current = builder

        classDefinition.getProperties().forEach { field ->
            val name = field.propertyName.identifier
            val fieldType = classResolver.resolveTypeDescription(field.typeName, field.isArray)
            val actualType = classResolver.resolveTypeDescription(field.typeName, false)

            current = defineBaseField(current, name, fieldType, field.modifiers)

            current =
                if (!field.isArray) defineNonArraySetter(current, name, fieldType, field.modifiers.isFactor())
                else defineArraySetters(current, name, actualType, field.modifiers.isFactor())

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
            .defineField(internalFieldOf(name), fieldType, Modifier.PUBLIC)
            .annotateField(ModifierMetadata(modifiers))
    }

    private fun defineNonArraySetter(
        builder: DynamicType.Builder<*>,
        name: String,
        fieldType: TypeDescription,
        isFactor: Boolean,
    ): DynamicType.Builder<*> {
        val setterName = internalSetterOf(name)

        val appender = ValueSetterAppender(
            fieldName = internalFieldOf(name),
            propertyNameLiteral = name,
            isFactor = isFactor,
            intrinsicsObjectClass = jvmSetterIntrinsics, // Kotlin object
        )

        return builder
            .defineMethod(setterName, Void.TYPE, Modifier.PUBLIC)
            .withParameters(fieldType, TypeDescription.ForLoadedType.of(Boolean::class.java))
            .intercept(Implementation.Simple(appender))
    }

    private fun defineArraySetters(
        builder: DynamicType.Builder<*>,
        name: String,
        actualType: TypeDescription,
        isFactor: Boolean
    ): DynamicType.Builder<*> {
        val setterName = internalArrayListSetterOf(name)

        val appender = ValueSetterAppender(
            fieldName = internalFieldOf(name),
            propertyNameLiteral = name,
            isFactor = isFactor,
            intrinsicsObjectClass = jvmSetterIntrinsics, // Kotlin object
        )

        val intercept = MethodCall.invoke(jvmSetterIntrinsics.getCheckTypesMethod())
            .onField(jvmSetterIntrinsics.JVM_SETTER_INSTANCE_FIELD)
            .withArgument(0)
            .with(actualType)
            .andThen(Implementation.Simple(appender))

        val result = builder
            .defineMethod(setterName, Void.TYPE, Modifier.PUBLIC)
            .withParameters(ArrayList::class.java, Boolean::class.java)
            .intercept(intercept)

        return result
    }
}
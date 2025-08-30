package com.github.tanokun.addon.intermediate.generator

import com.github.tanokun.addon.definition.dynamic.ClassDefinition
import com.github.tanokun.addon.definition.dynamic.DynamicClass
import com.github.tanokun.addon.intermediate.DynamicJavaClassLoader
import com.github.tanokun.addon.intermediate.integrity.SetArrayListAdvice
import com.github.tanokun.addon.intermediate.integrity.SetArrayListAdvice.FixedArrayComponentType
import com.github.tanokun.addon.intermediate.metadata.ModifierMetadata
import com.github.tanokun.addon.intermediate.metadata.MutableFieldMetadata
import net.bytebuddy.asm.Advice
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.implementation.StubMethod
import net.bytebuddy.implementation.bytecode.assign.Assigner
import java.lang.reflect.Modifier

/**
 * すべてのフィールド(ユーザ定義フィールド)を定義
 */
class FieldsDefiner(
    private val creator: DynamicJavaClassLoader,
) {
    fun defineFields(
        builder: DynamicType.Builder<out DynamicClass>,
        classDefinition: ClassDefinition,
    ): DynamicType.Builder<out DynamicClass> {
        var current = builder

        classDefinition.fields.forEach { field ->
            val fieldType = creator.getClassOrGenerateOrListFromAll(field.typeName, field.isArray)

            current = current
                .defineField(fieldOf(field.fieldName.identifier), fieldType, Modifier.PUBLIC)
                .annotateField(ModifierMetadata(field.modifier))
                .let { if (field.isProperty()) it.annotateField(MutableFieldMetadata()) else it }

            if (!field.isArray) return@forEach

            val actualType = creator.getClassOrGenerateOrListFromAll(field.typeName, false)

            val methodName = internalArrayListSetterOf(field.fieldName.identifier)

            val fieldMapping = Advice.OffsetMapping.ForField.Unresolved.WithImplicitType(
                TypeDescription.ForLoadedType.of(fieldType).asGenericType(),
                false,
                Assigner.Typing.DYNAMIC,
                fieldOf(field.fieldName.identifier),
            )

            current = current
                .defineMethod(methodName, Void.TYPE, Modifier.PUBLIC)
                .withParameters(ArrayList::class.java)
                .intercept(
                    Advice.withCustomMapping()
                        .bind(Advice.OffsetMapping.Factory.Simple(Advice.FieldValue::class.java, fieldMapping))
                        .bind(FixedArrayComponentType::class.java, actualType)
                        .to(SetArrayListAdvice::class.java)
                        .wrap(StubMethod.INSTANCE)
                )
                .annotateMethod(ModifierMetadata(field.modifier))
        }

        return current
    }
}
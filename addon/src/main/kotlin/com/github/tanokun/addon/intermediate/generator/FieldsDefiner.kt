package com.github.tanokun.addon.intermediate.generator

import ch.njol.skript.lang.TriggerItem
import com.github.tanokun.addon.definition.dynamic.ClassDefinition
import com.github.tanokun.addon.intermediate.DynamicJavaClassLoader
import com.github.tanokun.addon.intermediate.metadata.ModifierMetadata
import com.github.tanokun.addon.intermediate.metadata.MutableFieldMetadata
import com.github.tanokun.addon.definition.dynamic.DynamicClass
import net.bytebuddy.dynamic.DynamicType
import java.lang.reflect.Modifier

/**
 * すべてのフィールド(ユーザ定義フィールド + 内部トリガーフィールド)を定義
 */
class FieldsDefiner(
    private val creator: DynamicJavaClassLoader,
) {
    fun defineAllFields(
        builder: DynamicType.Builder<out DynamicClass>,
        classDefinition: ClassDefinition,
    ): DynamicType.Builder<out DynamicClass> {
        var current = builder

        // ユーザ定義フィールド
        classDefinition.fields.forEach { field ->
            val fieldType = creator.getClassOrGenerateOrListFromAll(field.typeName, field.isArray)
            current = current
                .defineField(fieldOf(field.fieldName.identifier), fieldType, Modifier.PUBLIC)
                .annotateField(ModifierMetadata(field.modifier))
                .let { if (field.isMutable) it.annotateField(MutableFieldMetadata()) else it }
        }

        // 関数トリガー用フィールド
        classDefinition.functions.forEach { func ->
            current = current.defineField(
                internalFunctionTriggerFieldOf(func.name.identifier),
                TriggerItem::class.java,
                Modifier.PUBLIC or Modifier.STATIC
            )
        }

        // init トリガー用フィールド
        current = current.defineField(
            INTERNAL_INIT_TRIGGER_SECTION,
            TriggerItem::class.java,
            Modifier.PUBLIC or Modifier.STATIC
        )

        return current
    }
}
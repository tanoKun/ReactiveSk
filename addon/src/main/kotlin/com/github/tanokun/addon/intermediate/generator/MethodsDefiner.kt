package com.github.tanokun.addon.intermediate.generator

import ch.njol.skript.lang.TriggerItem
import ch.njol.skript.variables.Variables
import com.github.tanokun.addon.definition.dynamic.ClassDefinition
import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.definition.dynamic.function.FunctionDefinition
import com.github.tanokun.addon.intermediate.DynamicJavaClassLoader
import com.github.tanokun.addon.intermediate.integrity.SetArrayListAdvice
import com.github.tanokun.addon.intermediate.integrity.SetArrayListAdvice.FixedArrayComponentType
import com.github.tanokun.addon.intermediate.metadata.FunctionReturnTypeMetadata
import com.github.tanokun.addon.intermediate.metadata.ModifierMetadata
import com.github.tanokun.addon.intermediate.reduce.FunctionAdvice
import com.github.tanokun.addon.definition.dynamic.DynamicClass
import com.github.tanokun.addon.runtime.skript.function.call.mediator.RuntimeFunctionMediator
import com.github.tanokun.addon.runtime.skript.variable.internalTypedVariableOf
import net.bytebuddy.asm.Advice
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.implementation.MethodCall
import net.bytebuddy.implementation.StubMethod
import net.bytebuddy.implementation.bytecode.assign.Assigner
import org.bukkit.event.Event
import java.lang.reflect.Modifier

/**
 * 関数定義に基づくメソッド定義および内部補助メソッド(ArrayList セッタ)を定義
 */
class MethodsDefiner(
    private val creator: DynamicJavaClassLoader,
) {
    private val setVariableMethod = Variables::class.java.getMethod("setVariable", String::class.java, Any::class.java, Event::class.java, Boolean::class.java)

    fun defineAllMethods(
        builder: DynamicType.Builder<out DynamicClass>,
        classDefinition: ClassDefinition,
    ): DynamicType.Builder<out DynamicClass> {
        var current = builder

        // 関数定義
        classDefinition.functions.forEach { func ->
            val returnTypeName = (func.returnTypeName ?: Identifier("void")).identifier
            val returnTypeFieldName = internalFunctionReturnTypeFieldOf(func.name.identifier)

            current = current
                .defineField(returnTypeFieldName, Class::class.java, Modifier.PUBLIC or Modifier.STATIC)
                .annotateField(FunctionReturnTypeMetadata(returnTypeName))

            val parameterTypes = arrayOf(
                RuntimeFunctionMediator::class.java,
                *func.parameters
                    .map { creator.getClassOrGenerateOrListFromAll(it.typeName, it.isArray) }
                    .toTypedArray()
            )

            val impl = createMethodImplementation(func, classDefinition)

            current = current
                .defineMethod(func.name.identifier, Void.TYPE, Modifier.PUBLIC)
                .withParameters(*parameterTypes)
                .intercept(impl)
                .annotateMethod(ModifierMetadata(func.modifier))
        }

        classDefinition.fields.filter { it.isArray }.forEach { field ->
            val arrayType = creator.getClassOrGenerateOrListFromAll(field.typeName, true)
            val componentType = creator.getClassOrGenerateOrListFromAll(field.typeName)

            val methodName = internalArrayListSetterOf(field.fieldName.identifier)

            val fieldMapping = Advice.OffsetMapping.ForField.Unresolved.WithImplicitType(
                TypeDescription.ForLoadedType.of(arrayType).asGenericType(),
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
                        .bind(FixedArrayComponentType::class.java, componentType)
                        .to(SetArrayListAdvice::class.java)
                        .wrap(StubMethod.INSTANCE)
                )
                .annotateMethod(ModifierMetadata(field.modifier))
        }

        return current
    }

    private fun createMethodImplementation(
        func: FunctionDefinition,
        classDefinition: ClassDefinition,
    ): Implementation {
        val setThis: Implementation.Composable =
            createSetVariableToSkriptImplementation(Identifier("this"), false) { it.withThis() }

        val setArgs = func.parameters.foldIndexed(setThis) { index, acc, def ->
            val impl = createSetVariableToSkriptImplementation(def.parameterName, def.isArray) {
                it.withArgument(index + 1)
            }
            acc.andThen(impl)
        }

        val setFields = classDefinition.fields.fold(setArgs) { acc, def ->
            val impl = createSetVariableToSkriptImplementation(def.fieldName, def.isArray) {
                it.withField(fieldOf(def.fieldName.identifier))
            }
            acc.andThen(impl)
        }

        val body = setFields
            .andThen(
                MethodCall.invoke(TriggerItem::class.java.getMethod("walk", TriggerItem::class.java, Event::class.java))
                    .withField(internalFunctionTriggerFieldOf(func.name.identifier))
                    .withArgument(0)
            )

        val fieldMapping = Advice.OffsetMapping.ForField.Unresolved.WithImplicitType(
            TypeDescription.ForLoadedType.of(TriggerItem::class.java).asGenericType(),
            true,
            Assigner.Typing.STATIC,
            internalFunctionTriggerFieldOf(func.name.identifier)
        )

        return Advice.withCustomMapping()
            .bind(Advice.OffsetMapping.Factory.Simple(Advice.FieldValue::class.java, fieldMapping))
            .to(FunctionAdvice::class.java)
            .wrap(body)
    }

    private fun createSetVariableToSkriptImplementation(
        variableName: Identifier,
        isArray: Boolean,
        valueSet: (MethodCall) -> MethodCall
    ): Implementation.Composable {
        return MethodCall
            .invoke(setVariableMethod)
            .with(internalTypedVariableOf(variableName, 0))
            .let(valueSet)
            .withArgument(0)
            .with(true)
    }
}
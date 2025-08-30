package com.github.tanokun.addon.intermediate.generator

import ch.njol.skript.lang.TriggerItem
import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.definition.dynamic.ClassDefinition
import com.github.tanokun.addon.definition.dynamic.DynamicClass
import com.github.tanokun.addon.definition.dynamic.function.FunctionDefinition
import com.github.tanokun.addon.intermediate.DynamicJavaClassLoader
import com.github.tanokun.addon.intermediate.metadata.FunctionReturnTypeMetadata
import com.github.tanokun.addon.intermediate.metadata.ModifierMetadata
import com.github.tanokun.addon.intermediate.reduce.FunctionAdvice
import com.github.tanokun.addon.runtime.skript.function.call.mediator.RuntimeFunctionMediator
import com.github.tanokun.addon.runtime.variable.VariableFrames
import net.bytebuddy.asm.Advice
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.implementation.MethodCall
import net.bytebuddy.implementation.bytecode.assign.Assigner
import org.bukkit.event.Event
import java.lang.reflect.Modifier

/**
 * 関数定義に基づくメソッド定義および内部補助メソッド(ArrayList セッタ)を定義
 */
class MethodsDefiner(
    private val creator: DynamicJavaClassLoader,
) {
    private val setVariableMethod = VariableFrames::class.java.getMethod("set", Event::class.java, Int::class.java, Any::class.java)

    fun defineAllMethods(
        builder: DynamicType.Builder<out DynamicClass>,
        classDefinition: ClassDefinition,
    ): DynamicType.Builder<out DynamicClass> {
        var current = builder

        classDefinition.functions.forEach { func ->
            val returnTypeName = (func.returnTypeName ?: Identifier("void")).identifier
            val returnTypeFieldName = internalFunctionReturnTypeField(func.name.identifier)

            current = current
                .defineField(returnTypeFieldName, Class::class.java, Modifier.PUBLIC or Modifier.STATIC)
                .annotateField(FunctionReturnTypeMetadata(returnTypeName))

            val parameterTypes = arrayOf(
                RuntimeFunctionMediator::class.java,
                *func.parameters
                    .map { creator.getClassOrGenerateOrListFromAll(it.typeName, it.isArray) }
                    .toTypedArray()
            )

            val impl = createMethodImplementation(func)

            current = current
                .defineMethod(func.name.identifier, Void.TYPE, Modifier.PUBLIC)
                .withParameters(*parameterTypes)
                .intercept(impl)
                .annotateMethod(ModifierMetadata(func.modifier))
                .defineField(internalFunctionTriggerField(func.name.identifier), TriggerItem::class.java, Modifier.PUBLIC or Modifier.STATIC)
                .defineField(internalLocalsCapacityFieldOfFunction(func.name.identifier), Int::class.java, Modifier.PUBLIC or Modifier.STATIC)
        }

        return current
    }

    private fun createMethodImplementation(func: FunctionDefinition, ): Implementation {
        val beginFrameImpl: Implementation.Composable = MethodCall.invoke(VariableFrames::class.java.getMethod("beginFrame", Event::class.java, Int::class.java))
            .withArgument(0)
            .withField(INTERNAL_CONSTRUCTOR_LOCALS_CAPACITY)
            .andThen(createSetTypedVariableImplementation(0) { it.withThis() })

        val setArgsImpl = func.parameters.foldIndexed(beginFrameImpl) { index, acc, def ->
            val impl = createSetTypedVariableImplementation(index + 1) { it.withArgument(index + 1) }
            acc.andThen(impl)
        }

        val bodyImpl = setArgsImpl
            .andThen(
                MethodCall.invoke(TriggerItem::class.java.getMethod("walk", TriggerItem::class.java, Event::class.java))
                    .withField(internalFunctionTriggerField(func.name.identifier))
                    .withArgument(0)
            )

        val fieldMapping = Advice.OffsetMapping.ForField.Unresolved.WithImplicitType(
            TypeDescription.ForLoadedType.of(TriggerItem::class.java).asGenericType(),
            true,
            Assigner.Typing.STATIC,
            internalFunctionTriggerField(func.name.identifier)
        )

        return Advice.withCustomMapping()
            .bind(Advice.OffsetMapping.Factory.Simple(Advice.FieldValue::class.java, fieldMapping))
            .to(FunctionAdvice::class.java)
            .wrap(bodyImpl)
    }

    private fun createSetTypedVariableImplementation(
        index: Int,
        valueSet: (MethodCall) -> MethodCall
    ): Implementation.Composable {
        return MethodCall
            .invoke(setVariableMethod)
            .withArgument(0)
            .with(index)
            .let(valueSet)
    }
}
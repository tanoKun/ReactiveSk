package com.github.tanokun.addon.intermediate.generator

import ch.njol.skript.lang.TriggerItem
import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.definition.dynamic.ClassDefinition
import com.github.tanokun.addon.definition.dynamic.function.FunctionDefinition
import com.github.tanokun.addon.intermediate.metadata.MethodMetadata
import com.github.tanokun.addon.intermediate.reduce.FunctionAdvice
import com.github.tanokun.addon.module.ModuleManager
import com.github.tanokun.addon.runtime.skript.function.call.mediator.RuntimeFunctionMediator
import com.github.tanokun.addon.runtime.variable.AmbiguousVariableFrames
import net.bytebuddy.asm.Advice
import net.bytebuddy.description.annotation.AnnotationDescription
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.implementation.MethodCall
import net.bytebuddy.implementation.bytecode.assign.Assigner
import java.lang.reflect.Modifier

/**
 * 関数定義に基づくメソッド定義および内部補助メソッド(ArrayList セッタ)を定義
 */
class MethodsDefiner(
    private val moduleManager: ModuleManager
) {
    private val setVariableMethod = AmbiguousVariableFrames::class.java.getMethod("set", Any::class.java, Int::class.java, Any::class.java)

    fun defineAllMethods(
        builder: DynamicType.Builder<*>,
        classDefinition: ClassDefinition,
    ): DynamicType.Builder<*> {
        var current = builder

        classDefinition.functions.forEach { func ->
            val returnTypeName = func.returnTypeName ?: Identifier("void")
            val returnTypeDesc = moduleManager.resolveTypeDescription(returnTypeName)

            val parameterTypes = arrayOf(
                TypeDescription.ForLoadedType.of(RuntimeFunctionMediator::class.java),
                *func.parameters
                    .map { moduleManager.resolveTypeDescription(it.typeName, it.isArray) }
                    .toTypedArray()
            )

            val fakeParameterTypes = Array(func.parameters.size + 1) { Any::class.java }

            val processingImpl = createMethodImplementation(func)

            val argumentTypesAnnotation = AnnotationDescription.Builder
                .ofType(MethodMetadata::class.java)
                .define("returnType", returnTypeDesc)
                .defineTypeArray("argumentTypes", *parameterTypes)
                .define("modifiers", func.modifiers)
                .build()

            current = current
                .defineMethod(internalFunctionNameOf(func.name.identifier), Void.TYPE, Modifier.PUBLIC)
                .withParameters(*fakeParameterTypes)
                .intercept(processingImpl)
                .annotateMethod(argumentTypesAnnotation)
                .defineField(internalFunctionTriggerField(func.name.identifier), TriggerItem::class.java, Modifier.PUBLIC or Modifier.STATIC)
                .defineField(internalFunctionLocalsCapacityFieldOf(func.name.identifier), Int::class.java, Modifier.PUBLIC or Modifier.STATIC)
        }

        return current
    }

    private fun createMethodImplementation(func: FunctionDefinition): Implementation {
        val beginFrameImpl: Implementation.Composable = MethodCall.invoke(AmbiguousVariableFrames::class.java.getMethod("beginFrame", Any::class.java, Int::class.java))
            .withArgument(0)
            .withField(internalFunctionLocalsCapacityFieldOf(func.name.identifier))
            .andThen(createSetTypedVariableImplementation(0) { it.withThis() })

        val setArgsImpl = func.parameters.foldIndexed(beginFrameImpl) { index, acc, _ ->
            val impl = createSetTypedVariableImplementation(index + 1) { it.withArgument(index + 1) }
            acc.andThen(impl)
        }

        val fieldMapping = Advice.OffsetMapping.ForField.Unresolved.WithImplicitType(
            TypeDescription.ForLoadedType.of(TriggerItem::class.java).asGenericType(),
            true,
            Assigner.Typing.STATIC,
            internalFunctionTriggerField(func.name.identifier)
        )

        return Advice.withCustomMapping()
            .bind(Advice.OffsetMapping.Factory.Simple(Advice.FieldValue::class.java, fieldMapping))
            .to(FunctionAdvice::class.java)
            .wrap(setArgsImpl)
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
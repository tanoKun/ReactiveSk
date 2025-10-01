package com.github.tanokun.reactivesk.compiler.backend.codegen.method

import com.github.tanokun.reactivesk.compiler.backend.ClassResolver
import com.github.tanokun.reactivesk.compiler.backend.codegen.util.internalFunctionNameOf
import com.github.tanokun.reactivesk.compiler.backend.codegen.util.internalFunctionTriggerField
import com.github.tanokun.reactivesk.compiler.backend.intrinsics.*
import com.github.tanokun.reactivesk.compiler.backend.metadata.ModifierMetadata
import com.github.tanokun.reactivesk.lang.ClassDefinition
import net.bytebuddy.description.annotation.AnnotationDescription
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.implementation.FixedValue
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.implementation.MethodCall
import java.lang.reflect.Modifier

class MethodsGenerator(
    private val classResolver: ClassResolver,
    private val variableFramesIntrinsics: Class<out VariableFramesIntrinsics>,
    private val triggerItemIntrinsics: Class<out TriggerItemIntrinsics>,
    private val isImplementingBeginFrame: Boolean,
) {
    private val mediatorType = TypeDescription.ForLoadedType(Any::class.java)

    private val triggerType = TypeDescription.ForLoadedType(Any::class.java)

    fun defineAllMethods(
        builder: DynamicType.Builder<*>,
        classDefinition: ClassDefinition,
    ): DynamicType.Builder<*> {
        var current = builder

        classDefinition.functions.forEach { func ->
            val returns = func.returns
            val returnTypeDesc = classResolver.resolveTypeDescription(returns.typeName)
            val functionName = func.functionName.identifier

            val parameterTypes = arrayOf(
                mediatorType,
                *func.parameters
                    .map { classResolver.resolveTypeDescription(it.typeName, it.isArray) }
                    .toTypedArray()
            )

            val processingImpl = createMethodImplementation(func)

            val modifierMetadataAnnotation = AnnotationDescription.Builder
                .ofType(ModifierMetadata::class.java)
                .define("modifiers", func.modifiers)
                .build()

            current = current
                .defineMethod(internalFunctionNameOf(functionName), returnTypeDesc, Modifier.PUBLIC)
                .withParameters(*parameterTypes)
                .intercept(processingImpl)
                .annotateMethod(modifierMetadataAnnotation)
                .defineField(internalFunctionTriggerField(functionName), triggerType, Modifier.PUBLIC or Modifier.STATIC)
        }

        return current
    }

    private fun createMethodImplementation(func: ClassDefinition.Function): Implementation {
        val initialImpl: Implementation.Composable = if (isImplementingBeginFrame) {
            MethodCall.invoke(variableFramesIntrinsics.getBeginFrameMethod())
                .onField(variableFramesIntrinsics.VARIABLE_FRAMES_INSTANCE_FIELD)
                .withArgument(0)
                .with(func.parameters.size + 1)
                .andThen(createSetTypedVariableImplementation(0) { it.withThis() })
        } else {
            createSetTypedVariableImplementation(0) { it.withThis() }
        }

        val setArgsImpl = func.parameters.foldIndexed(initialImpl) { index, acc, _ ->
            val impl = createSetTypedVariableImplementation(index + 1) { it.withArgument(index + 1) }
            acc.andThen(impl)
        }

        val walkImpl = setArgsImpl.andThen(
            MethodCall.invoke(triggerItemIntrinsics.getWalkMethod())
                .onField(triggerItemIntrinsics.TRIGGER_ITEM_INSTANCE_FIELD)
                .withField(internalFunctionTriggerField(func.functionName.identifier))
                .withArgument(0)
        )

        return walkImpl
            .let {
                if (func.returns.typeName.identifier != "void")
                    it.andThen(FixedValue.nullValue())
                else it
            }
    }

    private fun createSetTypedVariableImplementation(
        index: Int,
        valueSet: (MethodCall) -> MethodCall
    ): Implementation.Composable {
        return MethodCall
            .invoke(variableFramesIntrinsics.getSetMethod())
            .onField(variableFramesIntrinsics.VARIABLE_FRAMES_INSTANCE_FIELD)
            .withArgument(0)
            .with(index)
            .let(valueSet)
    }

}
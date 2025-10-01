package com.github.tanokun.reactivesk.compiler.backend.codegen.constructor

import com.github.tanokun.reactivesk.compiler.backend.ClassResolver
import com.github.tanokun.reactivesk.compiler.backend.codegen.util.CONSTRUCTOR_TRIGGER_SECTION
import com.github.tanokun.reactivesk.compiler.backend.codegen.util.internalArrayListSetterOf
import com.github.tanokun.reactivesk.compiler.backend.codegen.util.internalSetterOf
import com.github.tanokun.reactivesk.compiler.backend.intrinsics.*
import com.github.tanokun.reactivesk.lang.ClassDefinition
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.implementation.MethodCall
import net.bytebuddy.matcher.ElementMatchers
import java.lang.reflect.Modifier

class ConstructorGenerator(
    private val classResolver: ClassResolver,
    private val variableFramesIntrinsics: Class<out VariableFramesIntrinsics>,
    private val triggerItemIntrinsics: Class<out TriggerItemIntrinsics>,
    private val isImplementingBeginFrame: Boolean,
) {
    private val mediatorType = TypeDescription.ForLoadedType(Any::class.java)

    fun defineConstructor(
        builder: DynamicType.Builder<*>,
        classDefinition: ClassDefinition,
    ): DynamicType.Builder<*> {
        val constructorParameters = classDefinition.constructor.parameters

        val parameterTypes = arrayOf(
            mediatorType,
            *constructorParameters
                .map { classResolver.resolveTypeDescription(it.typeName, it.isArray) }
                .toTypedArray()
        )

        val beginFrameImpl: Implementation.Composable =
            MethodCall.invoke(Any::class.java.getDeclaredConstructor()).onSuper()

        val setFieldToValueImpl = constructorParameters.foldIndexed(beginFrameImpl) { index, acc, parameter ->
            if (parameter !is ClassDefinition.Property.InitializedProperty) return@foldIndexed acc

            if (parameter.isArray)
                acc.andThen(getInternalArrayListSetter(parameter, index))
            else
                acc.andThen(getInternalFieldSetter(parameter, index))
        }

        return builder
            .defineField(CONSTRUCTOR_TRIGGER_SECTION, mediatorType, Modifier.PUBLIC or Modifier.STATIC)
            .defineConstructor(Modifier.PUBLIC)
            .withParameters(*parameterTypes)
            .intercept(setFieldToValueImpl.andThen(createConstructorProxyImplementation(constructorParameters)))
    }

    private fun createConstructorProxyImplementation(parameters: List<ClassDefinition.Constructor.Parameter>): Implementation.Composable {
        val initialImpl: Implementation.Composable =
            if (isImplementingBeginFrame)
                MethodCall.invoke(variableFramesIntrinsics.getBeginFrameMethod())
                    .onField(variableFramesIntrinsics.VARIABLE_FRAMES_INSTANCE_FIELD)
                    .withArgument(0)
                    .with(parameters.size + 1)
                    .andThen(createSetTypedVariableImplementation(0) { it.withThis() })
             else createSetTypedVariableImplementation(0) { it.withThis() }

        val setParamsImpl = parameters.foldIndexed(initialImpl) { index, acc, _ ->
            acc.andThen(createSetTypedVariableImplementation(index + 1) { it.withArgument(index + 1) })
        }

        return setParamsImpl.andThen(
            MethodCall.invoke(triggerItemIntrinsics.getWalkMethod())
                .onField(triggerItemIntrinsics.TRIGGER_ITEM_INSTANCE_FIELD)
                .withField(CONSTRUCTOR_TRIGGER_SECTION)
                .withArgument(0)
        )
    }

    private fun createSetTypedVariableImplementation(
        index: Int,
        valueSet: (MethodCall) -> MethodCall
    ): Implementation.Composable {
        return MethodCall.invoke(variableFramesIntrinsics.getSetMethod())
            .onField(variableFramesIntrinsics.VARIABLE_FRAMES_INSTANCE_FIELD)
            .withArgument(0)
            .with(index)
            .let(valueSet)
    }

    private fun getInternalArrayListSetter(parameter: ClassDefinition.Constructor.Parameter, index: Int) =
        MethodCall.invoke(ElementMatchers.named(internalArrayListSetterOf(parameter.parameterName.identifier)))
            .withArgument(index + 1)
            .with(false)

    private fun getInternalFieldSetter(parameter: ClassDefinition.Constructor.Parameter, index: Int) =
        MethodCall.invoke(ElementMatchers.named(internalSetterOf(parameter.parameterName.identifier)))
            .withArgument(index + 1)
            .with(false)
}
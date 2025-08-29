package com.github.tanokun.addon.intermediate.generator

import ch.njol.skript.lang.TriggerItem
import ch.njol.skript.variables.Variables
import com.github.tanokun.addon.definition.dynamic.ClassDefinition
import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.intermediate.DynamicJavaClassLoader
import com.github.tanokun.addon.intermediate.reduce.InitAdvice
import com.github.tanokun.addon.definition.dynamic.DynamicClass
import com.github.tanokun.addon.runtime.skript.init.mediator.RuntimeConstructorMediator
import com.github.tanokun.addon.runtime.skript.variable.internalTypedVariableOf
import net.bytebuddy.asm.Advice
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.implementation.FieldAccessor
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.implementation.MethodCall
import net.bytebuddy.matcher.ElementMatchers.named
import org.bukkit.event.Event
import java.lang.reflect.Modifier
import kotlin.jvm.java

/**
 * コンストラクタ本体と init 呼び出し(プロキシ)を定義
 */
class ConstructorDefiner(
    private val creator: DynamicJavaClassLoader,
) {
    private val setVariableMethod = Variables::class.java.getMethod("setVariable", String::class.java, Any::class.java, Event::class.java, Boolean::class.java)

    fun defineConstructor(
        builder: DynamicType.Builder<out DynamicClass>,
        classDefinition: ClassDefinition,
    ): DynamicType.Builder<out DynamicClass> {
        val ctorParams = classDefinition.constructorParameters

        val paramTypes = arrayOf(
            RuntimeConstructorMediator::class.java,
            *ctorParams.map { creator.getClassOrGenerateOrListFromAll(it.typeName, it.isArray) }.toTypedArray()
        )

        // super()
        val ctorImpl: Implementation.Composable =
            MethodCall.invoke(Object::class.java.getDeclaredConstructor()).onSuper()

        // 引数からプロパティへ代入(配列は内部セッタ経由)
        val setProps = ctorParams.foldIndexed(ctorImpl) { index, acc, p ->
            if (!p.isProperty) return@foldIndexed acc

            if (p.isArray) {
                val setterName = internalArrayListSetterOf(p.parameterName.identifier)
                acc.andThen(
                    MethodCall.invoke(named(setterName))
                        .withArgument(index + 1)
                )
            } else {
                acc.andThen(
                    FieldAccessor.ofField(fieldOf(p.parameterName.identifier))
                        .setsArgumentAt(index + 1)
                )
            }
        }.andThen(
            MethodCall.invoke(named(INTERNAL_CONSTRUCTOR_PROXY))
                .withAllArguments()
        )

        return builder
            .defineConstructor(Modifier.PUBLIC)
            .withParameters(*paramTypes)
            .intercept(setProps)
            .defineMethod(INTERNAL_CONSTRUCTOR_PROXY, Void.TYPE, Modifier.PRIVATE)
            .withParameters(*paramTypes)
            .intercept(createConstructorProxyImplementation(classDefinition))
    }

    private fun createConstructorProxyImplementation(classDefinition: ClassDefinition): Implementation {
        val ctorParams = classDefinition.constructorParameters

        val setThis: Implementation.Composable =
            createSetVariableToSkriptImplementation(Identifier("this")) { it.withThis() }

        val setParams = ctorParams.foldIndexed(setThis) { index, acc, p ->
            acc.andThen(
                createSetVariableToSkriptImplementation(p.parameterName) {
                    it.withArgument(index + 1)
                }
            )
        }

        val body = setParams.andThen(
            MethodCall.invoke(TriggerItem::class.java.getMethod("walk", TriggerItem::class.java, Event::class.java))
                .withField(INTERNAL_INIT_TRIGGER_SECTION)
                .withArgument(0)
        )

        return Advice.to(InitAdvice::class.java).wrap(body)
    }

    private fun createSetVariableToSkriptImplementation(
        variableName: Identifier,
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
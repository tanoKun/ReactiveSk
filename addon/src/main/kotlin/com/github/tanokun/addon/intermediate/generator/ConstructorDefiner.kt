package com.github.tanokun.addon.intermediate.generator

import ch.njol.skript.lang.TriggerItem
import com.github.tanokun.addon.definition.dynamic.ClassDefinition
import com.github.tanokun.addon.intermediate.reduce.InitAdvice
import com.github.tanokun.addon.module.ModuleManager
import com.github.tanokun.addon.runtime.skript.init.mediator.RuntimeConstructorMediator
import com.github.tanokun.addon.runtime.variable.AmbiguousVariableFrames
import net.bytebuddy.asm.Advice
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.implementation.FieldAccessor
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.implementation.MethodCall
import net.bytebuddy.matcher.ElementMatchers.named
import org.bukkit.event.Event
import java.lang.reflect.Modifier

/**
 * 本体コンストラクタと、そこから呼び出される疑似コンストラクタ (Method) を作成します。
 * [ClassDefinition.constructorParameters] で定義されているコンストラクタパラメーターのうち、プロパティであるものは
 * 本体コンストラクタで先に初期化されます。
 *
 * ## 本体コンストラクタ
 * 以下の順番で生成されます。
 * - super()
 * - プロパティ初期化
 * - 疑似コンストラクタの呼び出し
 *
 * ## 例
 * ```
 * class Test[val property1: string, argument1: string]:
 *     field:
 *         val field1: string
 * ```
 * このようなクラス定義では、以下のイメージでバイトコードが生成されます。
 * ```java
 * public class Test implements DynamicClass {
 *     @ModifierMetadata(modifiers = 1)
 *     public String property1;
 *     public static TriggerItem constructor;
 *
 *     public Test(RuntimeConstructorMediator var1, String var2, String var3) {
 *         super();
 *         this.property1 = var2;
 *         this.proxyConstructor(var1, var2, var3);
 *     }
 *
 *     private void proxyConstructor(RuntimeConstructorMediator var1, String var2, String var3) {
 *         VariableFrames.set(var1, 0, this);
 *         VariableFrames.set(var1, 1, var2);
 *         VariableFrames.set(var1, 2, var3);
 *
 *         TriggerItem.walk(constructor, var1);
 *     }
 * }
 * ```
 *
 * メソッド名や、フィールド名は実際と異なりますが、このようになります。
 * [AmbiguousVariableFrames] へのフレーム登録が行われていますが、注入される [Event] を基準にして
 * 自身インスタンスを `index 0` とし、そこから`コンストラクタパラメーター`が連番されます。
 *
 * skript 側で解析したものを挿入する際、
 * コンストラクタパラメーター分を加算するのを忘れないようにしてください。
 *
 */
class ConstructorDefiner(
    private val moduleManager: ModuleManager
) {
    private val setVariableMethod = AmbiguousVariableFrames::class.java.getMethod("set", Any::class.java, Int::class.java, Any::class.java)

    fun defineConstructor(
        builder: DynamicType.Builder<*>,
        classDefinition: ClassDefinition,
    ): DynamicType.Builder<*> {
        val ctorParams = classDefinition.constructorParameters

        val parameterTypes = arrayOf(
            TypeDescription.ForLoadedType.of(RuntimeConstructorMediator::class.java),
            *ctorParams
                .map { moduleManager.resolveTypeDescription(it.typeName, it.isArray) }
                .toTypedArray()
        )

        val beginFrameImpl: Implementation.Composable =
            MethodCall.invoke(Object::class.java.getDeclaredConstructor()).onSuper()

        val setProps = ctorParams.foldIndexed(beginFrameImpl) { index, acc, p ->
            if (!p.isProperty()) return@foldIndexed acc

            if (p.isArray) {
                val setterName = internalArrayListSetterOf(p.parameterName.identifier)
                acc.andThen(
                    MethodCall.invoke(named(setterName))
                        .withArgument(index + 1)
                        .with(false)
                )
            } else {
                acc.andThen(
                    FieldAccessor.ofField(internalFieldOf(p.parameterName.identifier))
                        .setsArgumentAt(index + 1)
                )
            }
        }.andThen(
            MethodCall.invoke(named(CONSTRUCTOR_PROXY))
                .withAllArguments()
        )

        return builder
            .defineConstructor(Modifier.PUBLIC)
            .withParameters(*parameterTypes)
            .intercept(setProps)
            .defineMethod(CONSTRUCTOR_PROXY, Void.TYPE, Modifier.PRIVATE)
            .withParameters(*parameterTypes)
            .intercept(createConstructorProxyImplementation(classDefinition))
            .defineField(CONSTRUCTOR_LOCALS_CAPACITY, Int::class.java, Modifier.PUBLIC or Modifier.STATIC)
            .defineField(CONSTRUCTOR_TRIGGER_SECTION, TriggerItem::class.java, Modifier.PUBLIC or Modifier.STATIC)
    }

    private fun createConstructorProxyImplementation(classDefinition: ClassDefinition): Implementation {
        val constructorParameters = classDefinition.constructorParameters

        val beginFrameImpl: Implementation.Composable = MethodCall.invoke(AmbiguousVariableFrames::class.java.getMethod("beginFrame", Any::class.java, Int::class.java))
            .withArgument(0)
            .withField(CONSTRUCTOR_LOCALS_CAPACITY)
            .andThen(createSetTypedVariableImplementation(0) { it.withThis() })

        val setParamsImpl = constructorParameters.foldIndexed(beginFrameImpl) { index, acc, p ->
            acc.andThen(createSetTypedVariableImplementation(index + 1) { it.withArgument(index + 1) })
        }

        val bodyImpl = setParamsImpl.andThen(
            MethodCall.invoke(TriggerItem::class.java.getMethod("walk", TriggerItem::class.java, Event::class.java))
                .withField(CONSTRUCTOR_TRIGGER_SECTION)
                .withArgument(0)
        )

        return Advice.to(InitAdvice::class.java).wrap(bodyImpl)
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
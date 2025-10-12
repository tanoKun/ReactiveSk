package com.github.tanokun.reactivesk.v263.attach.caller.method

import com.github.tanokun.reactivesk.v263.caller.method.ConstructorCaller
import com.github.tanokun.reactivesk.v263.caller.method.MethodCaller
import net.bytebuddy.ByteBuddy
import net.bytebuddy.ClassFileVersion
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.dynamic.scaffold.TypeValidation
import net.bytebuddy.implementation.MethodCall
import net.bytebuddy.implementation.bytecode.assign.Assigner
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * ByteBuddy を利用してメソッド、コンストラクタ呼び出し用の実行クラスを動的に生成します。
 */
object MethodCallerBytecodeGenerator {

    /**
     * 指定した [targetConstructor] を呼び出すための [ConstructorCaller] 実装を動的に生成します。
     *
     * @param targetConstructor 呼び出し対象のコンストラクタ
     *
     * @return 生成された動的クラスの [DynamicType.Unloaded] インスタンス (型は [ConstructorCaller] のサブタイプ)
     */
    @Suppress("UNCHECKED_CAST")
    fun generateClass(
        targetConstructor: Constructor<*>
    ): DynamicType.Unloaded<out ConstructorCaller> {
        val builder: DynamicType.Builder<out ConstructorCaller> = ByteBuddy(ClassFileVersion.JAVA_V8)
            .with(TypeValidation.DISABLED)
            .subclass(TypeDescription.ForLoadedType.of(ConstructorCaller::class.java))
            .method { it.name == "call" }
            .intercept(
                MethodCall.construct(targetConstructor)
                    .withArgument(0)
                    .withArgumentArrayElements(1, 0, targetConstructor.parameterTypes.size - 1)
                    .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC)
            )
            .modifiers(Modifier.PUBLIC) as DynamicType.Builder<out ConstructorCaller>

        return builder.make()
    }

    @Suppress("UNCHECKED_CAST")
        /**
         * 指定した [targetMethod] を呼び出すための [MethodCaller] 実装を動的に生成します。
         *
         * @param targetMethod 呼び出し対象のメソッド
         *
         * @return 生成された動的クラスの [DynamicType.Unloaded] インスタンス (型は [MethodCaller] のサブタイプ)
         */
    fun generateClass(
        targetMethod: Method
    ): DynamicType.Unloaded<out MethodCaller> {
        val builder: DynamicType.Builder<out MethodCaller> = ByteBuddy(ClassFileVersion.JAVA_V8)
            .with(TypeValidation.DISABLED)
            .subclass(TypeDescription.ForLoadedType.of(MethodCaller::class.java))
            .method { it.name == "call" }
            .intercept(
                MethodCall.invoke(targetMethod)
                    .onArgument(0)
                    .withArgument(1)
                    .withArgumentArrayElements(2, 0, targetMethod.parameterTypes.size - 1)
                    .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC)
            )
            .modifiers(Modifier.PUBLIC) as DynamicType.Builder<out MethodCaller>

        return builder.make()
    }
}
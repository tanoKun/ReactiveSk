package com.github.tanokun.addon.clazz.loader

import com.github.tanokun.addon.clazz.definition.ClassDefinition
import com.github.tanokun.addon.clazz.definition.Identifier
import com.github.tanokun.addon.clazz.invoke.DynamicInvokeFunction
import net.bytebuddy.ByteBuddy
import net.bytebuddy.ClassFileVersion
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder
import net.bytebuddy.matcher.ElementMatchers.named
import java.lang.reflect.Modifier
import kotlin.jvm.java

class ByteBuddyGenerator {

    private val invokeFunction = DynamicInvokeFunction()

    /**
     * 循環参照を解決するために、一時的なプレースホルダーとなるクラスを生成する。
     * @param typeName 生成するプレースホルダーの元となるクラス名。
     * @return 中身のないダミーのクラスオブジェクト。
     */
    fun createPlaceholder(typeName: Identifier): Class<*> {
        val fqcn = "com.github.tanokun.addon.generated.${typeName}_Placeholder"
        return ByteBuddy()
            .subclass(Any::class.java)
            .name(fqcn)
            .make()
            .load(javaClass.classLoader, ClassLoadingStrategy.Default.INJECTION)
            .loaded
    }

    /**
     * ClassDefinitionとTypeRepositoryを使って、実際に動作するクラスを生成する。
     * @param classDefinition 生成するクラスの設計図となるAST。
     * @param typeRepository フィールドやメソッドの型を解決するための依存関係。
     * @return 生成され、クラスローダーにロードされた `java.lang.Class` オブジェクト。
     */
    fun generateClass(
        classDefinition: ClassDefinition,
        typeRepository: TypeRepository
    ): Class<*> {
        val fqcn = "com.github.tanokun.addon.generated.${classDefinition.className}"

        var builder: DynamicType.Builder<*> = ByteBuddy(ClassFileVersion.JAVA_V8)
            .subclass(Any::class.java)
            .name(fqcn)
            .modifiers(Modifier.PUBLIC)

        for (field in classDefinition.fields) {
            val mangledFieldName = $$"reactiveSk$original$field$$${field.fieldName}"
            val fieldType = typeRepository.getGeneratedClass(field.typeName, field.isArray)

            builder = builder.defineField(mangledFieldName, fieldType, field.modifier)
        }

        for (func in classDefinition.functions) {
            val returnType = typeRepository.getGeneratedClass(func.returnTypeName ?: Identifier("void"))

            val parameterTypes = func.parameters
                .map { typeRepository.getGeneratedClass(it.typeName, it.isArray) }
                .toTypedArray()

            builder = builder.defineMethod(func.name.identifier, returnType, func.modifier)
                .withParameters(*parameterTypes)
                .intercept(MethodDelegation.to(invokeFunction))
        }

        return builder.make()
            .load(javaClass.classLoader, ClassLoadingStrategy.Default.INJECTION)
            .loaded
    }
}
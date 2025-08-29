package com.github.tanokun.addon.intermediate.generator

import com.github.tanokun.addon.definition.dynamic.ClassDefinition
import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.intermediate.DynamicJavaClassLoader
import com.github.tanokun.addon.definition.dynamic.DynamicClass
import net.bytebuddy.ByteBuddy
import net.bytebuddy.ClassFileVersion
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy
import java.io.File
import java.lang.reflect.Modifier

/**
 * ClassDefinition から ByteBuddy を用いて実行可能な DynamicClass を生成します。
 * フィールド定義・メソッド定義・コンストラクタ定義を組み立て、Skript の TriggerItem と連携します。
 */
class ByteBuddyGenerator {
    /**
     * 与えられた型名でプレースホルダークラスを生成します。
     * 依存解決のために一時的に使用します。
     * @param typeName 型名
     * @return 生成されたプレースホルダークラス
     */
    fun createPlaceholder(typeName: Identifier): Class<out DynamicClass> {
        val fqcn = "${typeName}_Placeholder"
        return ByteBuddy()
            .subclass(DynamicClass::class.java)
            .name(fqcn)
            .make()
            .load(javaClass.classLoader, ClassLoadingStrategy.Default.INJECTION)
            .loaded
    }

    /**
     * 指定の ClassDefinition から最終的なクラスを生成します。
     * @param classDefinition クラス定義
     * @param creator 生成時の依存解決に用いるローダー
     * @return 生成されたクラス
     */
    fun generateClass(classDefinition: ClassDefinition, creator: DynamicJavaClassLoader): Class<out DynamicClass> {
        val fqcn = "com.github.tanokun.addon.generated.${classDefinition.className.identifier}"

        val fieldsDefiner = FieldsDefiner(creator)
        val methodsDefiner = MethodsDefiner(creator)
        val constructorDefiner = ConstructorDefiner(creator)

        var builder: DynamicType.Builder<out DynamicClass> = ByteBuddy(ClassFileVersion.JAVA_V8)
            .subclass(DynamicClass::class.java)
            .name(fqcn)
            .modifiers(Modifier.PUBLIC)

        builder = fieldsDefiner.defineAllFields(builder, classDefinition)
        builder = methodsDefiner.defineAllMethods(builder, classDefinition)
        builder = constructorDefiner.defineConstructor(builder, classDefinition)

        return builder.make()
            .load(javaClass.classLoader, ClassLoadingStrategy.Default.INJECTION)
            .apply { saveIn(File("generated-classes")) }
            .loaded
    }
}
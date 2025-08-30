package com.github.tanokun.addon.intermediate

import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.definition.dynamic.DynamicClass
import com.github.tanokun.addon.intermediate.generator.ByteBuddyGenerator
import com.github.tanokun.addon.intermediate.metadata.FunctionReturnTypeMetadata
import java.lang.reflect.Modifier

/**
 * 識別子から既存の Java クラスを解決する関数型です。
 */
typealias ClassResolver = (Identifier) -> Class<*>?

class DynamicJavaClassLoader(
    private val classResolver: ClassResolver,
    private val generator: ByteBuddyGenerator,
    private val dynamicClassDefinitionLoader: DynamicClassDefinitionLoader
) {
    private val dynamicClasses = hashMapOf<Identifier, Class<out DynamicClass>>()

    fun getDynamicClassOrGenerate(typeName: Identifier): Class<out DynamicClass> {
        return getDynamicClassOrNull(typeName) ?: generateAndCacheClass(typeName)
    }

    fun getDynamicClassOrNull(typeName: Identifier): Class<out DynamicClass>? = dynamicClasses[typeName]

    fun getClassOrGenerateOrListFromAll(typeName: Identifier, isArray: Boolean = false): Class<*> {
        if (isArray) return ArrayList::class.java
        classResolver(typeName)?.let { return it }
        return dynamicClasses[typeName] ?: generateAndCacheClass(typeName)
    }

    fun getClassOrListOrNullFromAll(typeName: Identifier, isArray: Boolean = false): Class<*>? {
        if (isArray) return ArrayList::class.java
        classResolver(typeName)?.let { return it }

        return dynamicClasses[typeName]
    }

    private fun generateAndCacheClass(typeName: Identifier): Class<out DynamicClass> {
        val definition = dynamicClassDefinitionLoader.getClassDefinition(typeName)
            ?: throw ClassNotFoundException("Custom class definition not found: $typeName")

        val placeholder = generator.createPlaceholder(typeName)
        dynamicClasses[typeName] = placeholder

        val generatedClass = generator.generateClass(definition, this)
        dynamicClasses[typeName] = generatedClass

        initFunctionReturnTypeFields(generatedClass)

        return generatedClass
    }

    private fun initFunctionReturnTypeFields(clazz: Class<out DynamicClass>) {
        for (field in clazz.declaredFields) {
            if (!Modifier.isPublic(field.modifiers) || !Modifier.isStatic(field.modifiers)) continue
            if (field.type != Class::class.java) continue

            val meta = field.getAnnotation(FunctionReturnTypeMetadata::class.java) ?: continue
            val typeName = meta.typeName

            val resolved: Class<*>? = when {
                typeName.equals("void", ignoreCase = true) -> Void.TYPE
                typeName == clazz.simpleName -> clazz
                else -> classResolver(Identifier(typeName))
            }

            if (resolved != null) {
                field.set(null, resolved)
            }
        }
    }
}
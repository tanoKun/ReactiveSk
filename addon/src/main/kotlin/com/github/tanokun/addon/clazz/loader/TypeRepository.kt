package com.github.tanokun.addon.clazz.loader

import ch.njol.skript.registrations.Classes
import com.github.tanokun.addon.clazz.definition.ClassDefinition
import com.github.tanokun.addon.clazz.definition.Identifier
import java.util.concurrent.ConcurrentHashMap
import java.lang.reflect.Array as JavaArray

class TypeRepository(private val generator: ByteBuddyGenerator) {
    private val definitions = ConcurrentHashMap<Identifier, ClassDefinition>()
    private val generatedClasses = ConcurrentHashMap<Identifier, Class<*>>()

    val allClassIdentifiers: Set<Identifier> get() = definitions.keys

    /**
     * [フェーズ2] パースが完了したClassDefinitionをリポジトリに登録する。
     * @param classDef パース済みのClassDefinitionオブジェクト。
     */
    fun registerDefinition(classDef: ClassDefinition) {
        if (definitions.containsKey(classDef.className)) {
            println("Warning: Class '${classDef.className}' is already defined and will be overwritten.")
        }
        definitions[classDef.className] = classDef
    }

    /**
     * [フェーズ3] 指定された型名に対応するJava Classオブジェクトを取得する。
     * - キャッシュにあればそれを返す。
     * - なければ、登録済みの定義からクラスを動的に生成し、キャッシュして返す。
     * - 循環参照を解決するためのプレースホルダー機能も含む。
     * @param typeName 解決したい型名 (e.g., "A", "string", "Player")。
     * @param isArray その型が配列型かどうか。
     * @return 解決・生成された `java.lang.Class` オブジェクト。
     * @throws ClassNotFoundException 対応する定義が見つからない場合。
     */
    fun getGeneratedClass(typeName: Identifier, isArray: Boolean = false): Class<*> {
        resolveWellKnownType(typeName)?.let {
            return if (isArray) JavaArray.newInstance(it, 0).javaClass else it
        }

        val baseTypeName = typeName
        val baseClass = generatedClasses[baseTypeName] ?: generateAndCacheClass(baseTypeName)

        return if (isArray) JavaArray.newInstance(baseClass, 0).javaClass else baseClass
    }

    /**
     * 実際にクラスの生成をトリガーし、キャッシュに格納する内部関数。
     */
    private fun generateAndCacheClass(typeName: Identifier): Class<*> {
        val definition = definitions[typeName]
            ?: throw ClassNotFoundException("Custom class definition not found: $typeName")

        val placeholder = generator.createPlaceholder(typeName)
        generatedClasses[typeName] = placeholder

        println(">>> Generating class: $typeName")
        val generatedClass = generator.generateClass(definition, this)
        println("<<< Finished generating class: $typeName")

        generatedClasses[typeName] = generatedClass
        return generatedClass
    }

    /**
     * 既知の型（Javaの基本型、Bukkitのクラスなど）を解決するヘルパー関数。
     */
    private fun resolveWellKnownType(typeName: Identifier): Class<*>? {
        return when (typeName.identifier.lowercase()) {
            "string" -> String::class.java
            "long" -> Long::class.javaObjectType
            "int" -> Int::class.javaObjectType
            "boolean" -> Boolean::class.javaObjectType
            "void" -> Void.TYPE
            else -> null
        }
    }
}
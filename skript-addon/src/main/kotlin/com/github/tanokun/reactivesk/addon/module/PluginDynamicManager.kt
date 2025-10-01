package com.github.tanokun.reactivesk.addon.module

import com.github.tanokun.reactivesk.addon.module.classloader.DynamicClassLoader
import com.github.tanokun.reactivesk.addon.module.classloader.PluginDynamicClassDefinitionLoader
import com.github.tanokun.reactivesk.compiler.backend.codegen.JvmBytecodeGenerator
import com.github.tanokun.reactivesk.lang.Identifier
import com.github.tanokun.reactivesk.skriptadapter.common.dynamic.ChangedDifference
import com.github.tanokun.reactivesk.skriptadapter.common.dynamic.DynamicManager
import java.io.File

class PluginDynamicManager(private val scriptRootFolder: File, private val jvmBytecodeGenerator: JvmBytecodeGenerator): DynamicManager {
    private var currentClassLoader: DynamicClassLoader? = null
    private var loadedClasses: Map<Identifier, Class<*>> = emptyMap()
    val definitionLoader = PluginDynamicClassDefinitionLoader()

    override fun initialize() {
        this.loadedClasses = performFullLoad()
    }

    override fun reload(): List<ChangedDifference> {
        val oldClasses = this.loadedClasses
        val newClasses = performFullLoad()
        this.loadedClasses = newClasses
        return calculateDifferences(oldClasses, newClasses)
    }

    private fun performFullLoad(): Map<Identifier, Class<*>> {
        definitionLoader.loadAllClassesFrom(scriptRootFolder)

        val allDefinitions = definitionLoader.getAllDefinitions()

        val unloadedTypes = allDefinitions.map { definition ->
            jvmBytecodeGenerator.generateClass(definition)
        }

        val newClassLoader = DynamicClassLoader(this.javaClass.classLoader, unloadedTypes)
        this.currentClassLoader = newClassLoader

        return unloadedTypes.associate { unloadedType ->
            val loadedClass = newClassLoader.loadClass(unloadedType.typeDescription.name)
            Identifier(loadedClass.simpleName) to loadedClass
        }
    }

    override fun getLoadedClass(typeName: Identifier): Class<*>? {
        return loadedClasses[typeName]
    }

    private fun calculateDifferences(oldMap: Map<Identifier, Class<*>>, newMap: Map<Identifier, Class<*>>): List<ChangedDifference> {
        val changedDifferences = mutableListOf<ChangedDifference>()
        val allKeys = oldMap.keys union newMap.keys

        for (key in allKeys) {
            val oldClass = oldMap[key]
            val newClass = newMap[key]
            val changedDifference = when {
                newClass != null && oldClass == null -> ChangedDifference.Added(key, newClass)
                oldClass != null && newClass == null -> ChangedDifference.Removed(key, oldClass)
                newClass != null && oldClass != null -> ChangedDifference.Changed(key, oldClass, newClass)
                else -> null
            }
            changedDifference?.let { changedDifferences.add(it) }
        }
        return changedDifferences
    }
}
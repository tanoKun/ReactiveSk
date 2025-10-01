package com.github.tanokun.addon.module

import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.intermediate.generator.ByteBuddyGenerator
import com.github.tanokun.addon.module.classloader.DynamicClassDefinitionLoader
import com.github.tanokun.addon.module.classloader.DynamicClassLoader
import net.bytebuddy.description.annotation.AnnotationList
import net.bytebuddy.description.type.TypeDescription
import java.io.File
import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicLong

typealias ClassResolver = (Identifier) -> Class<*>?

class ModuleManager(
    private val scriptRootFolder: File,
    private val classResolver: ClassResolver
) {
    private var currentClassLoader: DynamicClassLoader? = null
    private var loadedClasses: Map<Identifier, Class<*>> = emptyMap()
    val definitionLoader = DynamicClassDefinitionLoader()
    private val reloadGeneration = AtomicLong(0)

    sealed interface Difference {
        val className: Identifier
        data class Added(override val className: Identifier, val new: Class<*>) : Difference
        data class Removed(override val className: Identifier, val previous: Class<*>) : Difference
        data class Changed(override val className: Identifier, val previous: Class<*>, val new: Class<*>) : Difference
    }

    fun initialize() {
        this.loadedClasses = performFullLoad()
    }

    fun reload(): List<Difference> {
        val oldClasses = this.loadedClasses
        val newClasses = performFullLoad()
        this.loadedClasses = newClasses
        return calculateDifferences(oldClasses, newClasses)
    }

    private fun performFullLoad(): Map<Identifier, Class<*>> {
        reloadGeneration.incrementAndGet()
        definitionLoader.loadAllClassesFrom(scriptRootFolder)

        val allDefinitions = definitionLoader.getAllDefinitions()
        val generator = ByteBuddyGenerator(this)

        val unloadedTypes = allDefinitions.map { definition ->
            generator.generateClass(definition)
        }

        val newClassLoader = DynamicClassLoader(this.javaClass.classLoader, unloadedTypes)
        this.currentClassLoader = newClassLoader

        return unloadedTypes.associate { unloadedType ->
            val loadedClass = newClassLoader.loadClass(unloadedType.typeDescription.name)
            Identifier(loadedClass.simpleName) to loadedClass
        }
    }

    fun resolveTypeDescription(typeName: Identifier, isArray: Boolean = false): TypeDescription {
        if (isArray) return TypeDescription.ForLoadedType.of(ArrayList::class.java)
        if (typeName.identifier == "void") return TypeDescription.ForLoadedType.of(Void.TYPE)

        loadedClasses[typeName]?.let { return TypeDescription.ForLoadedType.of(it) }
        classResolver(typeName)?.let { return TypeDescription.ForLoadedType.of(it) }

        val fqcn = "com.github.tanokun.addon.generated.$typeName"

        return SafeLatentTypeDescription(fqcn)
    }

    fun getLoadedClass(typeName: Identifier): Class<*>? {
        return loadedClasses[typeName]
    }

    private fun calculateDifferences(oldMap: Map<Identifier, Class<*>>, newMap: Map<Identifier, Class<*>>): List<Difference> {
        val differences = mutableListOf<Difference>()
        val allKeys = oldMap.keys union newMap.keys

        for (key in allKeys) {
            val oldClass = oldMap[key]
            val newClass = newMap[key]
            val difference = when {
                newClass != null && oldClass == null -> Difference.Added(key, newClass)
                oldClass != null && newClass == null -> Difference.Removed(key, oldClass)
                newClass != null && oldClass != null -> Difference.Changed(key, oldClass, newClass)
                else -> null
            }
            difference?.let { differences.add(it) }
        }
        return differences
    }
}

internal class SafeLatentTypeDescription(name: String) : TypeDescription.Latent(
    name,
    Modifier.PUBLIC,
    TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Any::class.java),
    emptyList()
) {
    override fun getDeclaringType(): TypeDescription = this
    override fun getInnerClassCount(): Int = 0
    override fun getDeclaredAnnotations(): AnnotationList = AnnotationList.Empty()
}
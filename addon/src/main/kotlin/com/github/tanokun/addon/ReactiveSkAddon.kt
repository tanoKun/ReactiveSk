package com.github.tanokun.addon

import ch.njol.skript.Skript
import ch.njol.skript.SkriptAddon
import ch.njol.skript.classes.ClassInfo
import ch.njol.skript.registrations.Classes
import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.intermediate.DynamicClassDefinitionLoader
import com.github.tanokun.addon.intermediate.DynamicJavaClassLoader
import com.github.tanokun.addon.intermediate.generator.ByteBuddyGenerator
import com.github.tanokun.addon.runtime.skript.serializer.DynamicInstanceSerializer
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import org.bukkit.plugin.java.JavaPlugin
import java.lang.invoke.MethodHandles

lateinit var coroutineScope: CoroutineScope private set

lateinit var job: Job private set

val dynamicClassDefinitionLoader = DynamicClassDefinitionLoader()
val dynamicJavaClassLoader = DynamicJavaClassLoader(
    ::classResolver,
    ByteBuddyGenerator(),
    dynamicClassDefinitionLoader
)

val lookup: MethodHandles.Lookup = MethodHandles.lookup()

private fun classResolver(className: Identifier): Class<*>? {
    return Classes.getClassInfoNoError(className.identifier.lowercase())?.c
}

class ReactiveSkAddon : JavaPlugin() {
    lateinit var addon: SkriptAddon
        private set

    override fun onEnable() {
        val exceptionHandler = CoroutineExceptionHandler { context, throwable ->
            logger.severe(throwable.stackTraceToString())
        }
        job = SupervisorJob()
        coroutineScope = CoroutineScope(minecraftDispatcher + exceptionHandler + job)

        ClassesRegister.registerAll()

        dynamicClassDefinitionLoader.loadAllClassesFrom(folder = Skript.getInstance().dataFolder)
        dynamicClassDefinitionLoader.getClassNames().forEach {
            try {
                val clazz = dynamicJavaClassLoader.getDynamicClassOrGenerate(it)
                Classes.registerClass(
                    ClassInfo(clazz, it.identifier.lowercase())
                        .name(it.identifier)
                        .user(it.identifier, it.identifier.lowercase())
                        .serializer(DynamicInstanceSerializer())
                )
            }catch (e: Throwable) {
                logger.warning("Failed to load class '${it.identifier}' -> ${e.message}")
            }
        }

        addon = Skript.registerAddon(this)
        addon.loadClasses("com.github.tanokun.addon")

        logger.info("ReactiveSk Addon has been enabled successfully!")
    }

    override fun onDisable() {
        logger.info("ReactiveSk Addon has been disabled.")
    }
}
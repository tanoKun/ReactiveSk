package com.github.tanokun.addon

import ch.njol.skript.Skript
import ch.njol.skript.SkriptAddon
import ch.njol.skript.classes.ClassInfo
import ch.njol.skript.registrations.Classes
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.definition.dynamic.DynamicClass
import com.github.tanokun.addon.module.ModuleManager
import com.github.tanokun.addon.runtime.skript.serializer.DynamicInstanceSerializer
import com.github.tanokun.addon.runtime.variable.AmbiguousVariableFrames
import kotlinx.coroutines.*
import org.bukkit.plugin.java.JavaPlugin

lateinit var coroutineScope: CoroutineScope private set

lateinit var job: Job private set

val moduleManager by lazy { ModuleManager(Skript.getInstance().dataFolder, ::classResolver) }

lateinit var plugin: ReactiveSkAddon private set

private fun classResolver(className: Identifier): Class<*>? {
    return Classes.getClassInfoNoError(className.identifier.lowercase())?.c
}

class ReactiveSkAddon : JavaPlugin() {
    lateinit var addon: SkriptAddon
        private set

    @Suppress("UNCHECKED_CAST")
    override fun onEnable() {
        plugin = this

        val exceptionHandler = CoroutineExceptionHandler { context, throwable ->
            logger.severe(throwable.stackTraceToString())
        }
        job = SupervisorJob()
        coroutineScope = CoroutineScope(minecraftDispatcher + exceptionHandler + job)

        ClassesRegister.registerAll()

        moduleManager.initialize()
        moduleManager.definitionLoader.getAllDefinitions().forEach {
            try {
                Classes.registerClass(
                    ClassInfo(moduleManager.getLoadedClass(it.className) as Class<out DynamicClass>, it.className.identifier.lowercase())
                        .name(it.className.identifier)
                        .user(it.className.identifier, it.className.identifier.lowercase())
                        .serializer(DynamicInstanceSerializer())
                )
            }catch (e: Throwable) {
                logger.warning("Failed to load class '${it.className.identifier}' -> ${e.message}")
            }
        }

        addon = Skript.registerAddon(this)
        addon.loadClasses("com.github.tanokun.addon")

        logger.info("ReactiveSk Addon has been enabled successfully!")

        launch {
            while (true) {
                delay(5000)
                println(AmbiguousVariableFrames.frames.size)
            }
        }
    }

    override fun onDisable() {
        logger.info("ReactiveSk Addon has been disabled.")
    }
}
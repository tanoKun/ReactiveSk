package com.github.tanokun.addon

import ch.njol.skript.Skript
import ch.njol.skript.SkriptAddon
import ch.njol.skript.classes.ClassInfo
import ch.njol.skript.registrations.Classes
import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.definition.dynamic.DynamicClass
import com.github.tanokun.addon.intermediate.TestClass
import com.github.tanokun.addon.module.ModuleManager
import com.github.tanokun.addon.runtime.skript.serializer.DynamicInstanceSerializer
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import net.bytebuddy.ByteBuddy
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.implementation.MethodCall
import net.bytebuddy.matcher.ElementMatchers
import net.bytebuddy.matcher.ElementMatchers.named
import net.bytebuddy.pool.TypePool
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

      init {
          val resolvers = PluginResolvers.fromClass(this::class.java)
          val locator = resolvers.locator
          val pool = resolvers.typePool

          val type = pool.describe("com.github.tanokun.addon.intermediate.TestClass").resolve()
          ByteBuddy()
              .redefine<Any>(
                  type,
                  locator
              )
              .method(named<MethodDescription>("test1").and(ElementMatchers.isStatic()))
              //.intercept(Advice.to(TestAdvice::class.java))
              .intercept(MethodCall.call {
                    println("Hooked!")
              })
              .make(TypePool.Default.of(this::class.java.classLoader))

          TestClass.test1()
      }

    /*
            ClassFileLocator.ForClassLoader.ofSystemLoader().locate("com.github.tanokun.addon.intermediate.TestClass")

            val type = pool.describe("com.github.tanokun.addon.intermediate.TestClass").resolve()
            ByteBuddy()
                .redefine<Any>(
                    type,
                    ClassFileLocator.ForJarFile.ofClassPath()
                )
                .method(named<MethodDescription>("test1").and(ElementMatchers.isStatic()))
                .intercept(Advice.to(TestAdvice::class.java))
                .make(pool)

                    ByteBuddy()
                        .redefine<Any>(
                            pool.describe("ch.njol.skript.ScriptLoader").resolve(),
                            ClassFileLocator.ForClassLoader.ofSystemLoader()
                        )
                        .method(named<MethodDescription>("loadScripts").and(ElementMatchers.isStatic()))
                        .intercept(Advice.to(TestAdvice::class.java))
                        .make(pool)*/

    @Suppress("UNCHECKED_CAST")
    override fun onEnable() {

        plugin = this

        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
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
    }

    override fun onDisable() {
        logger.info("ReactiveSk Addon has been disabled.")
    }
}
package com.github.tanokun.addon

import ch.njol.skript.Skript
import ch.njol.skript.SkriptAddon
import ch.njol.skript.classes.ClassInfo
import ch.njol.skript.classes.Parser
import ch.njol.skript.lang.ParseContext
import ch.njol.skript.registrations.Classes
import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import com.github.tanokun.addon.clazz.definition.Identifier
import com.github.tanokun.addon.clazz.loader.DynamicClassLoader
import com.github.tanokun.addon.instance.InstanceProperty
import com.github.tanokun.addon.instance.serializer.InstancePropertySerializer
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import org.bukkit.plugin.java.JavaPlugin
import kotlin.jvm.java

lateinit var coroutineScope: CoroutineScope private set

lateinit var job: Job private set

class ReactiveSkAddon : JavaPlugin() {
    lateinit var addon: SkriptAddon
        private set

    private val classLoader = DynamicClassLoader()

    override fun onEnable() {
        val exceptionHandler = CoroutineExceptionHandler { context, throwable ->
            logger.severe(throwable.stackTraceToString())
        }
        job = SupervisorJob()
        coroutineScope = CoroutineScope(minecraftDispatcher + exceptionHandler + job)

        classLoader.loadAllClassesFrom(folder = Skript.getInstance().dataFolder)

        addon = Skript.registerAddon(this)
        addon.loadClasses("com.github.tanokun.addon")

        Classes.registerClass(
            ClassInfo(InstanceProperty::class.java, "instanceproperty")
                .serializer(InstancePropertySerializer())
        )

        Classes.registerClass(
            ClassInfo(Identifier::class.java, "identifier")
                .user("([_a-zA-Z$][a-zA-Z0-9_$]*)")
                .parser(object : Parser<Identifier>() {
                    override fun parse(s: String, context: ParseContext): Identifier? {
                        if (s.matches("[_a-zA-Z$][a-zA-Z0-9_$]*".toRegex())) {
                            return Identifier(s)
                        }

                        return null
                    }

                    override fun toString(o: Identifier, flags: Int): String = o.toString()


                    override fun toVariableNameString(o: Identifier): String? = variableNamePattern

                    val variableNamePattern: String
                        get() = "[_a-zA-Z$][a-zA-Z0-9_$]*"
                })
        )

        logger.info("ReactiveSk Addon has been enabled successfully!")
    }

    override fun onDisable() {
        logger.info("ReactiveSk Addon has been disabled.")
    }
}

/*
```
class Person:
    field:
        val name: PersonName
        val age: PersonAge
        val job: array of PersonJob

    init:
        send "person name: %{_name}%" to console

    function sendName(test1: string, test2: long):: PersonName:
        send "person test1: %{_test1}%" to console

        fun return {_name}
```
 */
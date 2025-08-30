package com.github.tanokun.addon.clazz.definition.parse

import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.intermediate.DynamicClassDefinitionLoader
import com.github.tanokun.addon.intermediate.DynamicJavaClassLoader
import com.github.tanokun.addon.intermediate.generator.ByteBuddyGenerator
import com.github.tanokun.addon.runtime.skript.init.mediator.RuntimeConstructorMediator
import com.github.tanokun.addon.runtime.variable.VariableFrames
import org.bukkit.event.Event
import java.io.File
import kotlin.test.Test


class DynamicClassParserTest {
    val dynamicClassDefinitionLoader = DynamicClassDefinitionLoader()
    val classLoader = DynamicJavaClassLoader(
        ::resolveWellKnownType,
        ByteBuddyGenerator(),
        dynamicClassDefinitionLoader
    )

    @Test
    fun test()  {
        val scriptDir = File("test-scripts")
        scriptDir.mkdirs()
        File(scriptDir, "Character.sk").writeText("""
        class Character[val name: string]:
    """.trimIndent())
        File(scriptDir, "Player.sk").writeText("""
class Person[val name: PersonName, val age: PersonAge, val job: PersonJob]:
class PersonName[val name: string]:    
class PersonAge[val age: long]:
class PersonJob[val jobName: string]:
class Counter[var count: long]:
    function increment(count: long):
        [this].count -> [count] + 1
    """.trimIndent())

        dynamicClassDefinitionLoader.loadAllClassesFrom(scriptDir)


        println("--- Triggering instance creation ---")

        val test: Event = RuntimeConstructorMediator()

        classLoader.getDynamicClassOrGenerate(Identifier("Counter")).constructors[1].newInstance(test, 10L)

        VariableFrames.beginFrame(test, 1)
        VariableFrames.set(test, 0, "aaaa")

        scriptDir.deleteRecursively()
    }

    private fun resolveWellKnownType(typeName: Identifier): Class<*>? {
        classLoader.getDynamicClassOrNull(typeName)?.let { return it }

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
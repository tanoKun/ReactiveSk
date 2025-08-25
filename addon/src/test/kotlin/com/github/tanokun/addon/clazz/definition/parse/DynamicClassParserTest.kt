package com.github.tanokun.addon.clazz.definition.parse

import com.github.tanokun.addon.clazz.definition.Identifier
import com.github.tanokun.addon.clazz.loader.DynamicClassLoader
import java.io.File
import kotlin.test.Test

class DynamicClassParserTest {
    @Test
    fun test() {
        val scriptDir = File("test-scripts")
        scriptDir.mkdirs()
        File(scriptDir, "Character.sk").writeText("""
        class Character:
            field:
                val name: string
    """.trimIndent())
        File(scriptDir, "Player.sk").writeText("""
        class Player:
            field:
                val character_data: Character
                val health: long

            function test(aa: string):: string:
                send "%{_aaa::*}%" to console
    """.trimIndent())

        val classLoader = DynamicClassLoader()

        classLoader.loadAllClassesFrom(scriptDir)

        println("--- Triggering instance creation ---")
        val playerInstance = classLoader.createInstance(Identifier("Player"))

        println("\n--- Verification ---")
        println("Successfully created instance of: ${playerInstance.javaClass.name}")
        println("Fields of Player class:")
        playerInstance.javaClass.declaredFields.forEach {
            println("  - ${it.name}: ${it.type.name}")
        }

        println(playerInstance.javaClass.methods.first { it.name == "test" }.invoke(playerInstance, "aa"))

        scriptDir.deleteRecursively()
    }
}

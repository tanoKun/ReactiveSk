package com.github.tanokun.addon.clazz.definition.parse

import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.intermediate.generator.ByteBuddyGenerator
import com.github.tanokun.addon.intermediate.DynamicClassDefinitionLoader
import com.github.tanokun.addon.intermediate.DynamicJavaClassLoader
import com.github.tanokun.addon.runtime.skript.init.mediator.RuntimeConstructorMediator
import io.mockk.mockk
import java.io.File
import kotlin.jvm.java
import kotlin.test.Test

class DynamicClassParserTest {
    val dynamicClassDefinitionLoader = DynamicClassDefinitionLoader()
    val classLoader = DynamicJavaClassLoader(
        ::resolveWellKnownType,
        ByteBuddyGenerator(),
        dynamicClassDefinitionLoader
    )

    @Test
    fun test() {
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
class Test[private val test: array of string, test2: string, var test3: string]:
    field:
        var test4: string
        val test6: string
    init throws [ERROR]:
        send "%{_test}%" to console
        
    function test(testParam: string):: string throws [ERROR]:
        send "%{_test}%" to console
    """.trimIndent())

        dynamicClassDefinitionLoader.loadAllClassesFrom(scriptDir)


        println("--- Triggering instance creation ---")

        classLoader.getDynamicClassOrGenerate(Identifier("Test")).constructors[1].newInstance(mockk<RuntimeConstructorMediator>(), arrayListOf("test1"))
        classLoader.getDynamicClassOrGenerate(Identifier("Person")).constructors[1].newInstance(mockk<RuntimeConstructorMediator>(), arrayListOf("test1"))

      //  val playerInstance = classLoader.createInstance(Identifier("Person"), mockk<RuntimeConstructorMediator>(), "aaaa")
/*

        playerInstance::class.java
            .getMethod("test", RuntimeFunctionMediator::class.java, String::class.java, ArrayList::class.java)
            .invoke(playerInstance, NonSuspendRuntimeFunctionMediator(), "test1", arrayListOf("test2"))

*/

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
package com.github.tanokun.addon.clazz.loader

import com.github.tanokun.addon.SkriptClassDefinitionParser
import com.github.tanokun.addon.clazz.definition.ClassDefinition
import com.github.tanokun.addon.clazz.definition.Identifier
import com.github.tanokun.addon.clazz.definition.parse.DynamicClassParserVisitor
import com.github.tanokun.addon.clazz.definition.parse.SkriptClassDefinitionIndentLexer
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

class DynamicClassLoader {
    private val generator = ByteBuddyGenerator()
    private val typeRepository = TypeRepository(generator)

    fun loadAllClassesFrom(folder: File) {
        println("--- Phase 1: Parsing all sources from '${folder.path}' ---")
        val allDefinitions = parseAllFiles(folder)
        println("Parsing complete. Found ${allDefinitions.size} class definitions.\n")

        println("--- Phase 2: Registering all definitions ---")
        allDefinitions.forEach { typeRepository.registerDefinition(it) }
        println("Registration complete. Ready for lazy generation.\n")
    }


    fun createInstance(className: Identifier): Any {
        val generatedClass = typeRepository.getGeneratedClass(className)
        return generatedClass.getDeclaredConstructor().newInstance()
    }

    private fun parseAllFiles(folder: File): List<ClassDefinition> {
        if (!folder.exists() || !folder.isDirectory) {
            throw IllegalArgumentException("Provided folder does not exist or is not a directory: ${folder.path}")
        }

        val definitions = mutableListOf<ClassDefinition>()

        folder.walkTopDown().forEach { file ->
            if (file.isFile && file.extension == "sk") {
                try {
                    println("Parsing file: ${file.name}")
                    val fileContent = BufferedReader(InputStreamReader(FileInputStream(file), Charsets.UTF_8)).use { it.readText() }

                    val lexer = SkriptClassDefinitionIndentLexer(CharStreams.fromString(fileContent))
                    val tokens = CommonTokenStream(lexer)
                    val parser = SkriptClassDefinitionParser(tokens)
                    val tree = parser.program()

                    // TODO: エラーハンドリングをここに追加するとより堅牢になる
                    // parser.errorHandler = ...
                    // parser.removeErrorListeners()
                    // parser.addErrorListener(...)

                    val visitor = DynamicClassParserVisitor()

                    val definitionsInFile = tree.topLevelElement().mapNotNull { element ->
                        element.classDef()?.let { classDefTree ->
                            visitor.visit(classDefTree) as ClassDefinition
                        }
                    }

                    definitions.addAll(definitionsInFile)
                } catch (e: Exception) {
                    println("Error parsing file ${file.name}: ${e.message}")
                }
            }
        }
        return definitions
    }
}
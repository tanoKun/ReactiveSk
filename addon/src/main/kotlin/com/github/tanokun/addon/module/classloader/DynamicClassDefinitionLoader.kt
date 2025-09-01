package com.github.tanokun.addon.module.classloader

import com.github.tanokun.addon.SkriptClassDefinitionParser
import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.definition.dynamic.ClassDefinition
import com.github.tanokun.addon.intermediate.parse.DynamicClassParserVisitor
import com.github.tanokun.addon.intermediate.parse.SkriptClassDefinitionIndentLexer
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.File

class DynamicClassDefinitionLoader {
    private val definitions = mutableMapOf<Identifier, ClassDefinition>()

    fun loadAllClassesFrom(folder: File) {
        clear()
        folder.walkTopDown().filter { it.isFile && it.extension == "sk" }.forEach { file ->
            try {
                parseAndCacheFile(file)
            } catch (e: Exception) {
                System.err.println("Failed to parse file ${file.name}: ${e.message}")
            }
        }
    }

    private fun parseAndCacheFile(file: File) {
        val definitionsInFile = parseSingleFile(file)
        definitionsInFile.forEach { def ->
            if (definitions.containsKey(def.className)) {
                System.err.println("Warning: Duplicate class definition for '${def.className}'. The definition from '${file.name}' will be used.")
            }
            definitions[def.className] = def
        }
    }

    fun getAllDefinitions(): List<ClassDefinition> {
        return definitions.values.toList()
    }

    fun getClassDefinition(className: Identifier): ClassDefinition? = definitions[className]

    private fun parseSingleFile(file: File): List<ClassDefinition> {
        val input = CharStreams.fromPath(file.toPath())
        val lexer = SkriptClassDefinitionIndentLexer(input)
        val tokens = CommonTokenStream(lexer)
        val parser = SkriptClassDefinitionParser(tokens)
        val tree = parser.program()
        val visitor = DynamicClassParserVisitor()
        return tree.classDef().map { visitor.visitClassDef(it) }
    }

    private fun clear() {
        definitions.clear()
    }
}
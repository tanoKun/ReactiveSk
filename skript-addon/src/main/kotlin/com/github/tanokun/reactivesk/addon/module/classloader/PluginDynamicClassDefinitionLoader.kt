package com.github.tanokun.reactivesk.addon.module.classloader

import com.github.tanokun.reactivesk.compiler.frontend.parser.DynamicClassDefinitionIndentLexer
import com.github.tanokun.reactivesk.compiler.frontend.parser.DynamicClassDefinitionVisitor
import com.github.tanokun.reactivesk.compiler.frontend.parser.antlr.DynamicClassDefinitionParser
import com.github.tanokun.reactivesk.lang.ClassDefinition
import com.github.tanokun.reactivesk.lang.Identifier
import com.github.tanokun.reactivesk.skriptadapter.common.dynamic.DynamicClassDefinitionLoader
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.File

class PluginDynamicClassDefinitionLoader: DynamicClassDefinitionLoader {
    private val definitions = mutableMapOf<Identifier, ClassDefinition>()

    override fun loadAllClassesFrom(folder: File) {
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

    override fun getAllDefinitions(): List<ClassDefinition> {
        return definitions.values.toList()
    }

    override fun getClassDefinition(className: Identifier): ClassDefinition? = definitions[className]

    private fun parseSingleFile(file: File): List<ClassDefinition> {
        val input = CharStreams.fromPath(file.toPath())
        val lexer = DynamicClassDefinitionIndentLexer(input)
        val tokens = CommonTokenStream(lexer)
        val parser = DynamicClassDefinitionParser(tokens)
        val visitor = DynamicClassDefinitionVisitor()

        return visitor.visitProgram(parser.program())
    }

    private fun clear() {
        definitions.clear()
    }
}
package com.github.tanokun.addon.clazz.definition.parse.field

import SkriptClassDefinitionLexer
import SkriptClassDefinitionParser
import ch.njol.skript.config.SectionNode
import com.github.tanokun.addon.clazz.definition.Identifier
import com.github.tanokun.addon.clazz.definition.field.FieldDefinition
import com.github.tanokun.addon.clazz.definition.parse.ParserErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

class FieldDefinitionParser(private val fieldNode: SectionNode) {
    fun parseField(): List<ParseFieldResult> {
        val results = mutableListOf<ParseFieldResult>()

        for (fieldLineNode in fieldNode) {
            val result = parseLine(fieldLineNode.key ?: "")

            results.add(result)
        }

        return results
    }

    private fun parseLine(line: String): ParseFieldResult {
        val charStream = CharStreams.fromString(line)
        val lexer = SkriptClassDefinitionLexer(charStream)
        val tokenStream = CommonTokenStream(lexer)
        val parser = SkriptClassDefinitionParser(tokenStream)

        val errorListener = ParserErrorListener().apply {
            parser.removeErrorListeners()
            parser.addErrorListener(this)
        }

        val tree = parser.field()

        if (errorListener.hasErrors()) {
            val combinedMessage = errorListener.errors.joinToString("\n")
            return ParseFieldResult.Failure(combinedMessage, line)
        }

        parser.fieldDefinition()
        val defCtx = tree.fieldDefinition()

        val isMutable = defCtx.VAR() != null
        val isArray = defCtx.ARRAY() != null
        val fieldName = defCtx.IDENTIFIER(0).text
        val typeName = defCtx.IDENTIFIER(1).text

        return ParseFieldResult.Success(FieldDefinition(Identifier(fieldName), typeName, isMutable, isArray))
    }
}
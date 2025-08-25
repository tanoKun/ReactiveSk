package com.github.tanokun.addon.clazz.definition.parse.function

import SkriptClassDefinitionLexer
import SkriptClassDefinitionParser
import com.github.tanokun.addon.clazz.definition.function.FunctionDefinition
import com.github.tanokun.addon.clazz.definition.parse.ParserErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

class FunctionParser {

    fun parseSignature(signature: String): ParseFunctionResult {
        val charStream = CharStreams.fromString(signature)
        val lexer = SkriptClassDefinitionLexer(charStream)
        val tokenStream = CommonTokenStream(lexer)
        val parser = SkriptClassDefinitionParser(tokenStream)

        // デフォルトのエラー出力を抑制し、カスタムリスナーでエラーを収集
        parser.removeErrorListeners()
        val errorListener = ParserErrorListener()
        parser.addErrorListener(errorListener)

        // パースを実行
        val tree = parser.signature()

        // 構文エラーがあれば、それをFailureとして返す
        if (errorListener.errors.isNotEmpty()) {
            return ParseFunctionResult.Failure(errorListener.errors.joinToString("\n"))
        }

        // Visitorを使ってパースツリーからデータを抽出
        val visitor = FunctionSignatureVisitor()
        val data = visitor.visit(tree)

        return ParseFunctionResult.Success(data as FunctionDefinition)
    }
}
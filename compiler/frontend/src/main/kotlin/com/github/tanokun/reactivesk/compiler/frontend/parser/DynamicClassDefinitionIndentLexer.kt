package com.github.tanokun.reactivesk.compiler.frontend.parser

import com.github.tanokun.reactivesk.compiler.frontend.parser.antlr.DynamicClassDefinitionLexer
import com.github.tanokun.reactivesk.compiler.frontend.parser.antlr.DynamicClassDefinitionParser
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.TokenSource
import org.antlr.v4.runtime.misc.Pair
import java.util.*

/**
 * インデントベースのブロック構造を INDENT/DEDENT トークンとして供給するラッパーレクサーです。
 * 入力の改行ごとに次行の空白量を計測し、インデント差分に応じてトークンを注入します。
 */
class DynamicClassDefinitionIndentLexer(input: CharStream) : DynamicClassDefinitionLexer(input) {

    private val pendingTokens = LinkedList<Token>()
    private val indents = Stack<Int>().apply { push(0) }

    private var hitEOF = false

    override fun nextToken(): Token {
        if (pendingTokens.isNotEmpty()) {
            return pendingTokens.poll()
        }

        val next = super.nextToken()

        return if (hitEOF) {
            next
        } else {
            processToken(next)
        }
    }

    private fun processToken(token: Token): Token {
        when (token.type) {
            Token.EOF -> {
                hitEOF = true
                while (indents.peek() != 0) {
                    pendingTokens.add(createDedentToken())
                    indents.pop()
                }
                pendingTokens.add(token)
            }
            DynamicClassDefinitionParser.NEWLINE -> {
                pendingTokens.add(token)
                handleNewLine()
            }
            else -> {
                pendingTokens.add(token)
            }
        }
        return pendingTokens.poll()
    }

    private fun handleNewLine() {
        var lookahead = 1
        var nextChar = inputStream.LA(lookahead).toChar()
        var indent = 0
        while (nextChar == ' ' || nextChar == '\t') {
            indent++
            lookahead++
            nextChar = inputStream.LA(lookahead).toChar()
        }

        val lastIndent = indents.peek()

        if (indent > lastIndent) {
            pendingTokens.add(createIndentToken())
            indents.push(indent)
        } else if (indent < lastIndent) {
            while (indent < indents.peek()) {
                pendingTokens.add(createDedentToken())
                indents.pop()
            }
        }
    }

    private fun createIndentToken(): Token = commonToken(DynamicClassDefinitionParser.INDENT)
    private fun createDedentToken(): Token = commonToken(DynamicClassDefinitionParser.DEDENT)

    private fun commonToken(type: Int): Token {
        val start = _tokenStartCharIndex
        val stop = _tokenStartCharIndex
        val line = _tokenStartLine
        val charPositionInLine = _tokenStartCharPositionInLine

        return _factory.create(
            Pair(this as TokenSource, _input as CharStream),
            type,
            "",
            _channel,
            start,
            stop,
            line,
            charPositionInLine
        )
    }
}
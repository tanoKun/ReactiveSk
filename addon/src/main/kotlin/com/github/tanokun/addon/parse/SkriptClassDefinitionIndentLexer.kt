package com.github.tanokun.addon.parse

import com.github.tanokun.addon.SkriptClassDefinitionLexer
import com.github.tanokun.addon.SkriptClassDefinitionParser
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.TokenSource
import org.antlr.v4.runtime.misc.Pair
import java.util.*

/**
 * インデントベースのブロック構造を INDENT/DEDENT トークンとして供給するラッパーレクサーです。
 * 入力の改行ごとに次行の空白量を計測し、インデント差分に応じてトークンを注入します。
 */
class SkriptClassDefinitionIndentLexer(input: CharStream) : SkriptClassDefinitionLexer(input) {

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
                // 残っているインデントをすべて DEDENT として吐く
                while (indents.peek() != 0) {
                    pendingTokens.add(createDedentToken())
                    indents.pop()
                }
                // 最後に EOF
                pendingTokens.add(token)
            }
            SkriptClassDefinitionParser.NEWLINE -> {
                // NEWLINE 自体は出力
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
        // 次行のインデント幅を数える(スペース/タブのみ)
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
        // 同一インデント幅の場合は何もしない
    }

    private fun createIndentToken(): Token = commonToken(SkriptClassDefinitionParser.INDENT)
    private fun createDedentToken(): Token = commonToken(SkriptClassDefinitionParser.DEDENT)

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
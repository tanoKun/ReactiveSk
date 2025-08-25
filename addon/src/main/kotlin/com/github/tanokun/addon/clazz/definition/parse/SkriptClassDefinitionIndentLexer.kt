package com.github.tanokun.addon.clazz.definition.parse

import com.github.tanokun.addon.SkriptClassDefinitionLexer
import com.github.tanokun.addon.SkriptClassDefinitionParser
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.TokenSource
import org.antlr.v4.runtime.misc.Pair
import java.util.LinkedList
import java.util.Stack

class SkriptClassDefinitionIndentLexer(input: CharStream) : SkriptClassDefinitionLexer(input) {

    // 生成されたINDENT/DEDENTトークンを一時的に保持するキュー
    private val pendingTokens = LinkedList<Token>()
    // 現在のインデントレベルを保持するスタック
    private val indents = Stack<Int>().apply { push(0) }

    private var hitEOF = false

    override fun nextToken(): Token {
        // もしキューにトークンがあれば、それを先に返す
        if (pendingTokens.isNotEmpty()) {
            return pendingTokens.poll()
        }

        // 次のトークンを取得
        val next = super.nextToken()

        return if (hitEOF) {
            next // EOFに到達したら、以降は通常の動作に戻す
        } else {
            processToken(next)
        }
    }

    private fun processToken(token: Token): Token {
        when (token.type) {
            Token.EOF -> {
                // ファイルの終端に到達した場合
                hitEOF = true
                // 残っているインデントをすべてDEDENTトークンとして生成
                while (indents.peek() != 0) {
                    pendingTokens.add(createDedentToken())
                    indents.pop()
                }
                // 最後にEOFトークン自体を追加
                pendingTokens.add(token)
            }
            SkriptClassDefinitionParser.NEWLINE -> {
                // 改行トークンを処理
                pendingTokens.add(token) // NEWLINEはそのままキューへ
                handleNewLine()
            }
            else -> {
                // その他の通常のトークン
                pendingTokens.add(token)
            }
        }
        return pendingTokens.poll()
    }

    private fun handleNewLine() {
        // 次の行のインデントを計算する
        // ANTLRのLexerが持つinputStreamから直接次の文字を「覗き見」する
        var nextChar = inputStream.LA(1).toChar()
        var indent = 0
        while (nextChar == ' ' || nextChar == '\t') {
            indent++
            nextChar = inputStream.LA(indent + 1).toChar()
        }

        val lastIndent = indents.peek()

        if (indent > lastIndent) {
            // インデントが増えた -> INDENTトークンを生成
            pendingTokens.add(createIndentToken())
            indents.push(indent)
        } else if (indent < lastIndent) {
            // インデントが減った -> DEDENTトークンを生成
            while (indent < indents.peek()) {
                pendingTokens.add(createDedentToken())
                indents.pop()
            }
        }
        // インデントが変わらない場合は何もしない
    }

    private fun createIndentToken(): Token = commonToken(SkriptClassDefinitionParser.INDENT)
    private fun createDedentToken(): Token = commonToken(SkriptClassDefinitionParser.DEDENT)

    private fun commonToken(type: Int): Token {
        // 現在のレキサーの状態から、仮想トークンの位置情報を決定
        val start = _tokenStartCharIndex
        val stop = _tokenStartCharIndex
        val line = _tokenStartLine
        val charPositionInLine = _tokenStartCharPositionInLine

        // 正しい引数で TokenFactory.create を呼び出す
        return _factory.create(
            Pair(this as TokenSource, _input as CharStream), // source: Pair<TokenSource, CharStream>
            type,           // type: Int
            "",             // text: String (仮想トークンなので空)
            _channel,       // channel: Int
            start,          // start: Int
            stop,           // stop: Int
            line,           // line: Int
            charPositionInLine // charPositionInLine: Int
        )
    }
}
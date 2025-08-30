package com.github.tanokun.addon.intermediate

import com.github.tanokun.addon.SkriptClassDefinitionParser
import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.definition.dynamic.ClassDefinition
import com.github.tanokun.addon.parse.DynamicClassParserVisitor
import com.github.tanokun.addon.parse.SkriptClassDefinitionIndentLexer
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.atn.ATNConfigSet
import org.antlr.v4.runtime.dfa.DFA
import org.antlr.v4.runtime.misc.ParseCancellationException
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

/**
 * 構文エラー発生時に必ず例外を送出するエラーリスナーです。
 */
private object ThrowingErrorListener : BaseErrorListener() {
    override fun syntaxError(
        recognizer: Recognizer<*, *>,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        msg: String,
        e: RecognitionException?
    ) {
        throw ParseCancellationException("Syntax error at line $line:$charPositionInLine: $msg")
    }
}

/**
 * クラス定義ファイルをパースし、ClassDefinition を管理します。
 * 解析時にはインデント対応レクサーと厳格なエラーハンドリングを用います。
 */
class DynamicClassDefinitionLoader {
    private val definitions = ConcurrentHashMap<Identifier, ClassDefinition>()

    /**
     * 指定フォルダ配下のファイルを走査し、すべてのクラス定義を読み込みます。
     * @param folder ルートフォルダ
     */
    fun loadAllClassesFrom(folder: File) {
        val allDefinitions = parseAllFiles(folder)
        allDefinitions.forEach {
            definitions[it.className] = it
        }
    }

    /**
     * 指定フォルダ配下の .sk ファイルをすべてパースします。
     * @param folder ルートフォルダ
     * @return 読み込んだクラス定義の一覧
     * @throws IllegalArgumentException フォルダが存在しないまたはディレクトリでない場合
     */
    private fun parseAllFiles(folder: File): List<ClassDefinition> {
        if (!folder.exists() || !folder.isDirectory) {
            throw IllegalArgumentException("Provided folder does not exist or is not a directory: ${folder.path}")
        }

        val definitions = mutableListOf<ClassDefinition>()

        folder.walkTopDown().forEach { file ->
            if (file.isFile && file.extension == "sk") {
                try {
                    val fileContent = BufferedReader(InputStreamReader(FileInputStream(file), Charsets.UTF_8)).use { it.readText() }
                    val lines = fileContent.split('\n')

                    val input = CharStreams.fromString(fileContent)

                    val lexer = SkriptClassDefinitionIndentLexer(input).apply {
                        removeErrorListeners()
                        addErrorListener(MinimalErrorListener(file.name, lines, showLineSnippet = true))
                    }

                    val tokens = CommonTokenStream(lexer)

                    val parser = SkriptClassDefinitionParser(tokens).apply {
                        removeErrorListeners()
                        addErrorListener(ThrowingErrorListener)
                        addErrorListener(MinimalErrorListener(file.name, lines, showLineSnippet = true))
                        errorHandler = BailErrorStrategy()
                    }

                    val tree = parser.program()

                    val visitor = DynamicClassParserVisitor()
                    val definitionsInFile = tree.classDef().map {
                        visitor.visitClassDef(it)
                    }

                    definitions.addAll(definitionsInFile)
                } catch (e: ParseCancellationException) {
                    System.err.println("Parse error in '${file.name}': ${e.message}")
                } catch (e: RecognitionException) {
                    System.err.println("Recognition error in '${file.name}': ${e.message}")
                } catch (e: Exception) {
                    println("Error parsing file ${file.name}: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
        return definitions
    }

    /**
     * 型名に対応するクラス定義を取得します。
     * @param typeName 型名
     * @return 対応するクラス定義または null
     */
    fun getClassDefinition(typeName: Identifier): ClassDefinition? = definitions[typeName]

    /**
     * 読み込まれているクラス名の集合を返します。
     * @return クラス名の集合
     */
    fun getClassNames(): Set<Identifier> = definitions.keys
}

/**
 * 構文エラーを簡潔に整形して例外として報告するリスナーです。
 * 行スニペットとキャレット表示に対応します。
 */
private class MinimalErrorListener(
    private val sourceName: String,
    private val lines: List<String>,
    private val showLineSnippet: Boolean = true
) : BaseErrorListener() {
    /**
     * 構文エラーを整形して例外として送出します。
     * @param recognizer レコグナイザー
     * @param offendingSymbol 問題のトークン
     * @param line 行番号
     * @param charPositionInLine 行内位置
     * @param msg エラーメッセージ
     * @param e 例外情報
     */
    override fun syntaxError(
        recognizer: Recognizer<*, *>,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        msg: String,
        e: RecognitionException?
    ) {
        val tokenText = (offendingSymbol as? Token)?.text?.let { sanitize(it) } ?: "<?>"
        val near = tokenText.ifBlank { "\\n" }

        val base = "Syntax error at line $line:$charPositionInLine near '$near'"

        if (!showLineSnippet) {
            throw ParseCancellationException(base)
        }

        val lineText = lines.getOrNull(line - 1)?.let { sanitize(it) } ?: ""
        val caret = buildString {
            repeat(maxOf(0, charPositionInLine)) { append(' ') }
            append('^')
        }

        val message = buildString {
            append(base)
            if (lineText.isNotEmpty()) {
                append("\n")
                append(lineText.take(200))
                append("\n")
                append(caret)
            }
            // 必要なら msg を末尾に付けたい場合は次の2行を有効化
            // append("\n")
            // append(msg)
        }

        throw ParseCancellationException(message)
    }

    /**
     * 文脈依存の報告時に例外を送出します。
     * @param recognizer レコグナイザー
     * @param dfa DFA
     * @param startIndex 開始インデックス
     * @param stopIndex 終了インデックス
     * @param prediction 予測番号
     * @param configs 設定セット
     */
    override fun reportContextSensitivity(
        recognizer: Parser?,
        dfa: DFA?,
        startIndex: Int,
        stopIndex: Int,
        prediction: Int,
        configs: ATNConfigSet?
    ) {
        throw ParseCancellationException("aaaaaaaaaaaa")
    }

    /**
     * 表示用に制御文字を可視化します。
     * @param s 入力文字列
     * @return 可視化後の文字列
     */
    private fun sanitize(s: String): String {
        return s.replace("\r", "\\r")
            .replace("\n", "\\n")
            .replace("\t", "\\t")
    }
}
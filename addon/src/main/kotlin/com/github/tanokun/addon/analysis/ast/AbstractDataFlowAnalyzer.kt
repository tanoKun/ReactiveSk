package com.github.tanokun.addon.analysis.ast

import ch.njol.skript.sections.SecLoop
import com.github.tanokun.addon.analysis.ast.result.Diagnostic

/**
 * データフロー解析フレームワークの抽象基底クラス。
 *
 * @param T 解析中に各コード地点で伝播させる「状態」の型。
 *          この型はBooleanのようなプリミティブラッパーでも、
 *          複数の情報を持つdata classでも良い。
 */
abstract class AbstractDataFlowAnalyzer<T : Any>(private val rootAst: AstSection.Block) {

    /**
     * サブクラスが定義する、解析開始時の初期状態。
     */
    protected abstract val initialState: T

    /**
     * 1行の文(Line)を解析し、状態を遷移させる。
     * @param node 解析対象のLineノード。
     * @param currentState この行の直前の状態。
     * @return この行を解析した結果（診断と、通過後の最終状態）。
     */
    protected abstract fun analyzeLine(node: AstSection.Line, currentState: T): AnalysisResult<T>

    /**
     * 複数の分岐パスの状態をマージするルールを定義する。
     * @param statesToMerge 各分岐パスが完了した後の状態のリスト。
     * @return マージ後の新しい単一の状態。
     */
    protected abstract fun mergeBranchStates(statesToMerge: List<T>): T

    /**
     * ループ後の状態をマージするルールを定義する。
     * @param initialState ループに入る前の状態。
     * @param loopBodyFinalState ループ本体を1回以上通過した後の状態。
     * @return ループ全体が完了した後の最終的な状態。
     */
    protected abstract fun mergeLoopStates(initialState: T, loopBodyFinalState: T): T

    /**
     * 解析完了後、最終的な状態を使って追加の検証を行う。
     * @param rootNode 解析対象だったASTのルートノード。
     * @param finalState 解析完了後の最終的な状態。
     * @return 追加の検証で見つかった診断のリスト。
     */
    protected abstract fun verify(rootNode: AstSection.Block, finalState: T): List<Diagnostic>

    /**
     * ASTノードの解析結果をラップする。
     * @param diagnostics 発見された問題点。
     * @param finalState このノード完了後の最終的な状態。
     */
    data class AnalysisResult<T>(
        val diagnostics: List<Diagnostic>,
        val finalState: T
    )

    /**
     * 解析を実行し、最終的な診断結果を返す。
     * このメソッドはfinalとし、解析の全体的な流れを規定する（Template Method パターン）。
     */
    fun analyze():  AnalysisResult<T> {
        val finalResult = analyzeNode(rootAst, initialState)

        return finalResult.copy(diagnostics = finalResult.diagnostics + verify(rootAst, finalResult.finalState))
    }

    /**
     * ASTノードを再帰的に解析するメインディスパッチャ。
     */
    private fun analyzeNode(node: AstSection, currentState: T): AnalysisResult<T> {
        return when (node) {
            is AstSection.Line -> analyzeLine(node, currentState)
            is AstSection.Block -> analyzeBlock(node, currentState)
            is AstSection.Section -> analyzeSection(node, currentState)
            is AstSection.If -> analyzeIf(node, currentState)
        }
    }

    /**

     * 文のブロックを解析する。状態をシーケンシャルに伝播させる。
     */
    private fun analyzeBlock(node: AstSection.Block, initialState: T): AnalysisResult<T> {
        val totalDiagnostics = mutableListOf<Diagnostic>()
        // foldを使って、状態(state)を雪だるま式に更新していく
        val finalState = node.elements.fold(initialState) { currentState, element ->
            val result = analyzeNode(element, currentState)
            totalDiagnostics.addAll(result.diagnostics)
            // 次の要素の解析には、この要素の解析後の状態を使う
            result.finalState
        }
        return AnalysisResult(totalDiagnostics, finalState)
    }

    /**
     * Sectionノードを解析する。Loopかどうかで処理を分岐する。
     */
    private fun analyzeSection(node: AstSection.Section, currentState: T): AnalysisResult<T> {
        // SecLoopは特別扱いし、サブクラスのマージルールを呼び出す
        if (node.header is SecLoop) {
            val loopBodyResult = analyzeBlock(AstSection.Block(node.elements), currentState)
            val finalState = mergeLoopStates(currentState, loopBodyResult.finalState)
            return AnalysisResult(loopBodyResult.diagnostics, finalState)
        }
        // それ以外のSectionは、通常のBlockとして中身を解析
        return analyzeBlock(AstSection.Block(node.elements), currentState)
    }

    /**
     * if-elseif-elseチェーンを解析する。状態を分岐・マージする。
     */
    private fun analyzeIf(node: AstSection.If, initialState: T): AnalysisResult<T> {
        val thenResult = analyzeNode(node.thenSection, initialState)
        val elseIfResults = node.elseIfSections.map { analyzeNode(it.thenSection, initialState) }
        val elseResult = node.elseSection?.let { analyzeNode(it, initialState) }

        val allDiagnostics = (
            thenResult.diagnostics +
                elseIfResults.flatMap { it.diagnostics } +
                (elseResult?.diagnostics ?: emptyList())
            )

        val finalState: T
        if (elseResult == null) {
            val statesToMerge = listOf(initialState) + (listOf(thenResult) + elseIfResults).map { it.finalState }
            finalState = mergeBranchStates(statesToMerge)
        } else {
            val statesToMerge = (listOf(thenResult) + elseIfResults + listOf(elseResult)).map { it.finalState }
            finalState = mergeBranchStates(statesToMerge)
        }
        return AnalysisResult(allDiagnostics, finalState)
    }
}
package com.github.tanokun.addon.analysis.section.function

import ch.njol.skript.effects.EffExit
import com.github.tanokun.addon.analysis.ast.AbstractDataFlowAnalyzer
import com.github.tanokun.addon.analysis.ast.AstSection
import com.github.tanokun.addon.analysis.ast.result.Diagnostic
import com.github.tanokun.addon.analysis.ast.result.Severity
import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.runtime.skript.function.FunctionReturnEffect

/**
 * function セクションに対するデータフロー解析を行い、返り値の検証します。
 *
 * 以下の場合エラーとなります
 * - 確定した返り値がない場合
 *
 * @return [analyze] は検出した診断（エラー/警告）を時系列で返す
 */
class FunctionReturnAnalyzer(
    functionBodyAst: AstSection.Block,
    private val functionName: Identifier,
    private val className: Identifier,
    private val hasReturnValue: Boolean
) : AbstractDataFlowAnalyzer<Boolean>(functionBodyAst) {

    /**
     * 解析開始時、パスはまだ終了していない。
     */
    override val initialState: Boolean = false

    /**
     * 1行の文を解析する。
     * @param currentState この行の直前のパスが終了していたか (true=終了済み)。
     * @return この行を通過した後のパスが終了しているか (true=終了済み)。
     */
    override fun analyzeLine(
        node: AstSection.Line,
        currentState: Boolean
    ): AnalysisResult<Boolean> {
        if (hasReturnValue && node.item is EffExit) {
            val diagnostic = Diagnostic("require return value in function '$functionName' in '$className' so cannot use 'exit effect'.", node, Severity.ERROR)

            return AnalysisResult(listOf(diagnostic), false)
        }

        val isTerminating = node.item is FunctionReturnEffect
        if (isTerminating) {
            return AnalysisResult(emptyList(), true)
        }

        return AnalysisResult(emptyList(), false)
    }

    /**
     * 複数の分岐パスの状態(Boolean)をマージする。
     */
    override fun mergeBranchStates(statesToMerge: List<Boolean>): Boolean {
        return statesToMerge.all { it }
    }

    /**
     * ループ後の状態をマージする。
     * ループは0回実行される可能性があるため、ループ後のパスが終了しているとは限らない。
     */
    override fun mergeLoopStates(initialState: Boolean, loopBodyFinalState: Boolean): Boolean {
        return initialState
    }

    /**
     * 最終的な検証を行う。
     */
    override fun verify(rootNode: AstSection.Block, finalState: Boolean): List<Diagnostic> {
        if (hasReturnValue && !finalState) {
            val errorMessage = "Function '$functionName' in '$className' must return a value on all paths."
            return listOf(Diagnostic(errorMessage, rootNode))
        }

        return emptyList()
    }
}
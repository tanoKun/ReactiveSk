package com.github.tanokun.reactivesk.compiler.frontend.analyze

import com.github.tanokun.reactivesk.compiler.frontend.analyze.ast.AbstractDataFlowAnalyzer
import com.github.tanokun.reactivesk.compiler.frontend.analyze.ast.AstNode
import com.github.tanokun.reactivesk.compiler.frontend.analyze.ast.result.Diagnostic
import com.github.tanokun.reactivesk.compiler.frontend.analyze.ast.result.Severity
import com.github.tanokun.reactivesk.lang.Identifier

/**
 * function セクションに対するデータフロー解析を行い、返り値の検証します。
 *
 * 以下の場合エラーとなります
 * - 確定した返り値がない場合
 *
 * @return [analyze] は検出した診断（エラー/警告）を時系列で返す
 */
class FunctionReturnAnalyzer<H: Any>(
    functionBodyAst: AstNode.Struct<H>,
    private val functionName: Identifier,
    private val className: Identifier,
    private val hasReturnValue: Boolean
) : AbstractDataFlowAnalyzer<Boolean, H>(functionBodyAst) {

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
        node: AstNode.Line<H>,
        currentState: Boolean
    ): AnalysisResult<Boolean, H> {
        if (hasReturnValue && node is AstNode.Line.Exit) {
            val diagnostic = Diagnostic(
                "require return value in function '$functionName' in '$className' so cannot use 'exit effect'.",
                node,
                Severity.ERROR
            )

            return AnalysisResult(listOf(diagnostic), false)
        }

        if (node is AstNode.Line.FunReturn) return AnalysisResult(emptyList(), true)

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
    override fun verify(rootNode: AstNode.Struct<H>, finalState: Boolean): List<Diagnostic<H>> {
        if (hasReturnValue && !finalState) {
            val errorMessage = "Function '$functionName' in '$className' must return a value on all paths."
            return listOf(Diagnostic(errorMessage, rootNode))
        }

        return emptyList()
    }
}
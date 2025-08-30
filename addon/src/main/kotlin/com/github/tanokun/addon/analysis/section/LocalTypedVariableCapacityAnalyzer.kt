package com.github.tanokun.addon.analysis.section

import com.github.tanokun.addon.analysis.ast.AbstractDataFlowAnalyzer
import com.github.tanokun.addon.analysis.ast.AstSection
import com.github.tanokun.addon.analysis.ast.result.Diagnostic
import com.github.tanokun.addon.definition.skript.variable.LocalTypedVariableDeclarationEffect
import kotlin.math.max

class LocalTypedVariableCapacityAnalyzer(rootAst: AstSection.Block) : AbstractDataFlowAnalyzer<Int>(rootAst) {
    override val initialState: Int
        get() = 0

    override fun analyzeLine(
        node: AstSection.Line,
        currentState: Int,
    ): AnalysisResult<Int> {
        if (node.item !is LocalTypedVariableDeclarationEffect) return AnalysisResult(emptyList(), currentState)

        return AnalysisResult(emptyList(), currentState + 1)
    }

    override fun mergeBranchStates(statesToMerge: List<Int>): Int {
        return statesToMerge.maxOrNull() ?: 0
    }

    override fun mergeLoopStates(initialState: Int, loopBodyFinalState: Int): Int {
        return max(initialState, loopBodyFinalState)
    }

    override fun verify(rootNode: AstSection.Block, finalState: Int, ): List<Diagnostic> { return emptyList() }
}
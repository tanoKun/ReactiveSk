package com.github.tanokun.addon.analysis.init

import ch.njol.skript.sections.SecLoop
import ch.njol.skript.sections.SecWhile
import com.github.tanokun.addon.analysis.ast.AstSection
import com.github.tanokun.addon.analysis.ast.result.Diagnostic
import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.definition.dynamic.field.FieldDefinition
import com.github.tanokun.addon.runtime.skript.init.ResolveTypedValueFieldEffect
import jdk.nashorn.internal.codegen.CompilerConstants.className

private typealias FieldName = Identifier

/**
 * 特定のコード地点における、フィールドの初期化状態を表現する。
 *
 * @param definitelyAssigned この地点に到達する全てのパスで、確実に初期化済みのフィールドの集合
 * @param possiblyAssigned この地点に到達するいずれかのパスで、初期化された可能性のあるフィールドの集合
 */
private data class AnalysisState(
    val definitelyAssigned: Set<FieldName>,
    val possiblyAssigned: Set<FieldName>
) {
    companion object {
        val initial = AnalysisState(emptySet(), emptySet())
    }
}

private data class AnalysisResult(
    val diagnostics: List<Diagnostic>,
    val finalState: AnalysisState
)

class InitSectionAnalyzer(
    private val initSectionAst: AstSection.Block,
    private val className: Identifier,
    private val requiredFields: List<FieldDefinition>,
) {

    fun analyze(): List<Diagnostic> {
        val result = analyzeNode(initSectionAst, AnalysisState.initial)

        val allDiagnostics = result.diagnostics.toMutableList()

        val finalGuaranteedFieldNames = result.finalState.definitelyAssigned

        requiredFields.forEach { requiredField ->
            val requiredName = requiredField.fieldName
            if (requiredName !in finalGuaranteedFieldNames) {
                val errorMessage =
                    "Field '$requiredName' must be initialized on all paths in the init section of class '$className'."
                allDiagnostics.add(Diagnostic(errorMessage, initSectionAst))
            }
        }
        return allDiagnostics
    }

    private fun analyzeNode(node: AstSection, currentState: AnalysisState): AnalysisResult {
        return when (node) {
            is AstSection.Line -> analyzeLine(node, currentState)
            is AstSection.Block -> analyzeBlock(node, currentState)
            is AstSection.Section -> {
                if (node.header is SecLoop) analyzeLoop(node, currentState)
                else analyzeBlock(AstSection.Block(node.elements), currentState)

            }
            is AstSection.If -> analyzeIf(node, currentState)
        }
    }

    private fun analyzeLine(node: AstSection.Line, currentState: AnalysisState): AnalysisResult {
        val effect = node.item as? ResolveTypedValueFieldEffect
            ?: return AnalysisResult(emptyList(), currentState)

        val fieldName = effect.fieldName

        if (fieldName in currentState.possiblyAssigned) {
            val diagnostic = Diagnostic("Field '$fieldName' in '$className' cannot be reassigned (already initialized on at least one path).", node)
            val newState = AnalysisState(
                definitelyAssigned = currentState.definitelyAssigned + fieldName,
                possiblyAssigned = currentState.possiblyAssigned + fieldName
            )
            return AnalysisResult(listOf(diagnostic), newState)
        }

        val newState = AnalysisState(
            definitelyAssigned = currentState.definitelyAssigned + fieldName,
            possiblyAssigned = currentState.possiblyAssigned + fieldName
        )
        return AnalysisResult(emptyList(), newState)
    }

    private fun analyzeBlock(node: AstSection.Block, initialState: AnalysisState): AnalysisResult {
        val totalDiagnostics = mutableListOf<Diagnostic>()
        var currentState = initialState

        for (element in node.elements) {
            val result = analyzeNode(element, currentState)
            totalDiagnostics.addAll(result.diagnostics)
            currentState = result.finalState
        }

        return AnalysisResult(totalDiagnostics, currentState)
    }

    private fun analyzeLoop(loopNode: AstSection.Section, initialState: AnalysisState): AnalysisResult {
        val loopBodyResult = analyzeBlock(AstSection.Block(loopNode.elements), initialState)

        val finalState = AnalysisState(
            definitelyAssigned = initialState.definitelyAssigned,
            possiblyAssigned = initialState.possiblyAssigned.union(loopBodyResult.finalState.possiblyAssigned)
        )
        return AnalysisResult(loopBodyResult.diagnostics, finalState)
    }

    private fun analyzeIf(node: AstSection.If, initialState: AnalysisState): AnalysisResult {
        val thenResult = analyzeNode(node.thenSection, initialState)
        val elseIfResults = node.elseIfSections.map { analyzeNode(it.thenSection, initialState) }
        val elseResult = node.elseSection?.let { analyzeNode(it, initialState) }

        val allDiagnostics = (
            thenResult.diagnostics +
                elseIfResults.flatMap { it.diagnostics } +
                (elseResult?.diagnostics ?: emptyList())
            )

        val finalState: AnalysisState
        if (elseResult == null) {
            val allPossibleEndStates = listOf(initialState) + (listOf(thenResult) + elseIfResults).map { it.finalState }

            finalState = AnalysisState(
                definitelyAssigned = initialState.definitelyAssigned,
                possiblyAssigned = allPossibleEndStates.map { it.possiblyAssigned }.reduce { acc, set -> acc.union(set) }
            )
        } else {
            val allBranchEndStates = (listOf(thenResult) + elseIfResults + listOf(elseResult)).map { it.finalState }

            finalState = AnalysisState(
                definitelyAssigned = allBranchEndStates.map { it.definitelyAssigned }.reduce { acc, set -> acc.intersect(set) },
                possiblyAssigned = allBranchEndStates.map { it.possiblyAssigned }.reduce { acc, set -> acc.union(set) }
            )
        }
        return AnalysisResult(allDiagnostics, finalState)
    }
}
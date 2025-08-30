package com.github.tanokun.addon.analysis.section.init

import com.github.tanokun.addon.analysis.ast.AbstractDataFlowAnalyzer
import com.github.tanokun.addon.analysis.ast.AstSection
import com.github.tanokun.addon.analysis.ast.result.Diagnostic
import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.definition.dynamic.field.FieldDefinition
import com.github.tanokun.addon.runtime.skript.init.ResolveTypedValueFieldEffect

typealias FieldName = Identifier

/**
 * initセクションの解析で使われる状態。
 * 確定的セットと可能性セットの2つの情報を保持する。
 */
data class InitAnalysisState(
    val definitelyAssigned: Set<FieldName>,
    val possiblyAssigned: Set<FieldName>
)

/**
 * init セクションに対するデータフロー解析を行い、必須プロパティの初期化妥当性と重複初期化を検証します。
 *
 * 以下の場合エラーとなります
 * - 初期化が必要なフィールドが、確定した初期化が行われていない場合
 * - フィールドの初期化が重複の可能性がある場合
 *
 * @param initSectionAst 解析対象の init セクション AST（ルートブロック）
 * @param className 診断メッセージに用いるクラス名
 * @param requiredFields 全経路での初期化が求められるフィールド一覧
 * @return [analyze] は検出した診断（エラー/警告）を時系列で返す
 */
class InitSectionAnalyzer(
    initSectionAst: AstSection.Block,
    private val className: Identifier,
    private val requiredFields: List<FieldDefinition>
) : AbstractDataFlowAnalyzer<InitAnalysisState>(initSectionAst) {

    /**
     * 解析開始時は、両方のセットが空。
     */
    override val initialState: InitAnalysisState = InitAnalysisState(emptySet(), emptySet())

    /**
     * 1行の文を解析する。
     */
    override fun analyzeLine(
        node: AstSection.Line,
        currentState: InitAnalysisState
    ): AnalysisResult<InitAnalysisState> {
        val effect = node.item as? ResolveTypedValueFieldEffect ?: return AnalysisResult(emptyList(), currentState)

        val fieldName = effect.fieldName

        if (fieldName in currentState.possiblyAssigned) {
            val diagnostic = Diagnostic("Field '$fieldName' in '$className' cannot be reassigned (already initialized on at least one path).", node)
            val newState = currentState.copy(
                definitelyAssigned = currentState.definitelyAssigned + fieldName,
                possiblyAssigned = currentState.possiblyAssigned + fieldName
            )
            return AnalysisResult(listOf(diagnostic), newState)
        }

        val newState = currentState.copy(
            definitelyAssigned = currentState.definitelyAssigned + fieldName,
            possiblyAssigned = currentState.possiblyAssigned + fieldName
        )
        return AnalysisResult(emptyList(), newState)
    }

    /**
     * ループ後の状態をマージする。
     */
    override fun mergeLoopStates(
        initialState: InitAnalysisState,
        loopBodyFinalState: InitAnalysisState
    ): InitAnalysisState {
        return InitAnalysisState(
            definitelyAssigned = initialState.definitelyAssigned,
            possiblyAssigned = initialState.possiblyAssigned.union(loopBodyFinalState.possiblyAssigned)
        )
    }

    /**
     * 複数の分岐パスの状態をマージする。
     */
    override fun mergeBranchStates(statesToMerge: List<InitAnalysisState>): InitAnalysisState {
        return InitAnalysisState(
            definitelyAssigned = statesToMerge.map { it.definitelyAssigned }.reduce { acc, set -> acc.intersect(set) },
            possiblyAssigned = statesToMerge.map { it.possiblyAssigned }.reduce { acc, set -> acc.union(set) }
        )
    }

    /**
     * 最終的な検証を行う。
     */
    override fun verify(rootNode: AstSection.Block, finalState: InitAnalysisState): List<Diagnostic> {
        val diagnostics = mutableListOf<Diagnostic>()
        val finalGuaranteedFieldNames = finalState.definitelyAssigned

        requiredFields.forEach { requiredField ->
            val requiredName = requiredField.fieldName
            if (requiredName !in finalGuaranteedFieldNames) {
                val errorMessage = "Field '$requiredName' must be initialized on all paths in the init section of class '$className'."
                diagnostics.add(Diagnostic(errorMessage, rootNode))
            }
        }
        return diagnostics
    }
}
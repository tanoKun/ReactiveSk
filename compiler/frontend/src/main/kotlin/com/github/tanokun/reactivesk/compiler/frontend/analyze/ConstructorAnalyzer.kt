package com.github.tanokun.reactivesk.compiler.frontend.analyze

import com.github.tanokun.reactivesk.compiler.frontend.analyze.ast.AbstractDataFlowAnalyzer
import com.github.tanokun.reactivesk.compiler.frontend.analyze.ast.AstNode
import com.github.tanokun.reactivesk.compiler.frontend.analyze.ast.result.Diagnostic
import com.github.tanokun.reactivesk.lang.ClassDefinition
import com.github.tanokun.reactivesk.lang.Identifier

typealias FieldName = Identifier

/**
 * initセクションの解析で使われる状態。
 * 確定的セットと可能性セットの2つの情報を保持する。
 */
data class ConstructorAnalysisState(
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
class ConstructorAnalyzer<H: Any>(
    initSectionAst: AstNode.Struct<H>,
    private val className: Identifier,
    private val requiredFields: List<ClassDefinition.Property.UninitializedProperty>
) : AbstractDataFlowAnalyzer<ConstructorAnalysisState, H>(initSectionAst) {

    /**
     * 解析開始時は、両方のセットが空。
     */
    override val initialState: ConstructorAnalysisState = ConstructorAnalysisState(emptySet(), emptySet())

    /**
     * 1行の文を解析する。
     */
    override fun analyzeLine(
        node: AstNode.Line<H>,
        currentState: ConstructorAnalysisState
    ): AnalysisResult<ConstructorAnalysisState, H> {
        val effect = node as? AstNode.Line.ResolveField ?: return AnalysisResult(emptyList(), currentState)

        val fieldName = effect.fieldName

        if (fieldName in currentState.possiblyAssigned) {
            val diagnostic = Diagnostic(
                "Field '$fieldName' in '$className' cannot be reassigned (already initialized on at least one path).",
                node
            )

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
        initialState: ConstructorAnalysisState,
        loopBodyFinalState: ConstructorAnalysisState
    ): ConstructorAnalysisState {
        return ConstructorAnalysisState(
            definitelyAssigned = initialState.definitelyAssigned,
            possiblyAssigned = initialState.possiblyAssigned.union(loopBodyFinalState.possiblyAssigned)
        )
    }

    /**
     * 複数の分岐パスの状態をマージする。
     */
    override fun mergeBranchStates(statesToMerge: List<ConstructorAnalysisState>): ConstructorAnalysisState {
        return ConstructorAnalysisState(
            definitelyAssigned = statesToMerge.map { it.definitelyAssigned }.reduce { acc, set -> acc.intersect(set) },
            possiblyAssigned = statesToMerge.map { it.possiblyAssigned }.reduce { acc, set -> acc.union(set) }
        )
    }

    /**
     * 最終的な検証を行う。
     */
    override fun verify(rootNode: AstNode.Struct<H>, finalState: ConstructorAnalysisState): List<Diagnostic<H>> {
        val diagnostics = mutableListOf<Diagnostic<H>>()
        val finalGuaranteedFieldNames = finalState.definitelyAssigned

        requiredFields.forEach { requiredField ->
            val requiredName = requiredField.propertyName
            if (requiredName !in finalGuaranteedFieldNames) {
                val errorMessage = "Field '$requiredName' must be initialized on all paths in the init section of class '$className'."
                diagnostics.add(Diagnostic(errorMessage, rootNode))
            }
        }
        return diagnostics
    }
}
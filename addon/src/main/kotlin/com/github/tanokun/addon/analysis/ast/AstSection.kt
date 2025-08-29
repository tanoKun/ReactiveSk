package com.github.tanokun.addon.analysis.ast

import ch.njol.skript.lang.TriggerItem
import ch.njol.skript.sections.SecConditional

/**
 * else if節を表現するためのヘルパーデータクラス。
 * @param header `else if ...:` の行に対応するSecConditionalノード。
 * @param thenSection `else if`ブロックの中身。
 */
data class ElseIf(val header: SecConditional, val thenSection: AstSection.Block)

/**
 * Skriptのコード構造を表現する、型安全な木構造（AST）。
 */
sealed interface AstSection {
    /**
     * if-elseif-elseの条件分岐ブロック全体を表すノード。
     * @param header `if ...:` の行に対応するSecConditionalノード。
     * @param thenSection `if`ブロックの中身。
     * @param elseIfSections 0個以上の`else if`節のリスト。
     * @param elseSection オプショナルな`else`節の中身。
     */
    data class If(
        val header: SecConditional,
        val thenSection: Block,
        val elseIfSections: List<ElseIf>,
        val elseSection: Block?
    ) : AstSection

    /**
     * `loop:`, `while:`, `on chat:` のような、ヘッダーを持つセクションを表すノード。
     * @param header セクションの開始行に対応するSectionノード。
     * @param elements セクションの中身の要素リスト。
     */
    data class Section(val header: ch.njol.skript.lang.Section, val elements: List<AstSection>) : AstSection

    /**

     * 中括弧 `{ ... }` のように、特定のヘッダーを持たないコードブロックを表すノード。
     * トリガーのルートや、if/loopの本体などがこれにあたる。
     * @param elements ブロックの中身の要素リスト。
     */
    data class Block(val elements: List<AstSection>) : AstSection

    /**
     * `send "hello"` のような、単一の行で完結するEffectやExpressionを表すノード。
     * @param item 行に対応するTriggerItemノード。
     */
    data class Line(val item: TriggerItem) : AstSection
}

/**
 * AST構築の結果全体をラップするクラス。
 * @param first 処理の起点となった最初のTriggerItem。
 * @param root 構築されたASTのルートノード。
 */
data class SkriptAst(val first: TriggerItem?, val root: AstSection.Block)

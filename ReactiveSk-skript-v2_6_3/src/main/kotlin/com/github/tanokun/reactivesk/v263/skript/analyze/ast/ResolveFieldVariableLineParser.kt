package com.github.tanokun.reactivesk.v263.skript.analyze.ast

import ch.njol.skript.lang.TriggerItem
import com.github.tanokun.reactivesk.compiler.frontend.analyze.ast.AstNode
import com.github.tanokun.reactivesk.skriptadapter.common.analyze.ast.parse.AstParseResult
import com.github.tanokun.reactivesk.skriptadapter.common.analyze.ast.parse.NodeParser
import com.github.tanokun.reactivesk.skriptadapter.common.analyze.ast.parse.ParseContext
import com.github.tanokun.reactivesk.v263.skript.runtime.instantiation.ResolveFieldValueEffect

/**
 * [ResolveFieldValueEffect] を解析して AST のフィールド解決行ノードを生成するパーサです。
 *
 * 解析結果として該当する行ノードを含む [AstParseResult] を返します。
 */
object ResolveFieldVariableLineParser: NodeParser<TriggerItem> {
    override val priority: Int = 1000

    override fun canHandle(item: TriggerItem): Boolean = item is ResolveFieldValueEffect

    /**
     * 指定した [item] を解析してフィールド解決行を生成します。
     *
     * @param item 解析対象のトリガーアイテム
     * @param context 解析に使用するコンテキスト
     *
     * @return 解析結果の [AstParseResult]
     */
    override fun parse(item: TriggerItem, context: ParseContext<TriggerItem>): AstParseResult {
        item as ResolveFieldValueEffect

        val sectionAst = AstNode.Line.ResolveField<TriggerItem>(
            handler = item,
            fieldName = item.fieldName,
        )

        return AstParseResult.Single(sectionAst, item.next)
    }
}
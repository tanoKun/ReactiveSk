package com.github.tanokun.reactivesk.v263.skript.analyze.ast

import ch.njol.skript.lang.TriggerItem
import com.github.tanokun.reactivesk.compiler.frontend.analyze.ast.AstNode
import com.github.tanokun.reactivesk.skriptadapter.common.analyze.ast.parse.AstParseResult
import com.github.tanokun.reactivesk.skriptadapter.common.analyze.ast.parse.NodeParser
import com.github.tanokun.reactivesk.skriptadapter.common.analyze.ast.parse.ParseContext
import com.github.tanokun.reactivesk.v263.skript.runtime.function.FunctionReturnEffect

/**
 * 関数の return 行を解析して AST の行ノードを生成するパーサです。
 *
 * [FunctionReturnEffect] をハンドルし、対応する [AstNode.Line.FunReturn] を生成します。
 */
object FunReturnLineParser: NodeParser<TriggerItem> {
    override val priority: Int = 1000

    override fun canHandle(item: TriggerItem): Boolean = item is FunctionReturnEffect

    /**
     * 指定した [item] を解析して関数の return 行ノードを生成します。
     *
     * @param item 解析対象のトリガーアイテム
     * @param context 解析に使用するコンテキスト
     *
     * @return 解析結果の [AstParseResult]
     */
    override fun parse(item: TriggerItem, context: ParseContext<TriggerItem>): AstParseResult {
        val sectionAst = AstNode.Line.FunReturn(
            handler = item
        )

        return AstParseResult.Single(sectionAst, item.next)
    }
}
package com.github.tanokun.reactivesk.v263.skript.analyze.ast

import ch.njol.skript.lang.TriggerItem
import ch.njol.skript.sections.SecLoop
import com.github.tanokun.reactivesk.compiler.frontend.analyze.ast.AstNode
import com.github.tanokun.reactivesk.skriptadapter.common.analyze.ast.parse.AstParseResult
import com.github.tanokun.reactivesk.skriptadapter.common.analyze.ast.parse.NodeParser
import com.github.tanokun.reactivesk.skriptadapter.common.analyze.ast.parse.ParseContext
import com.github.tanokun.reactivesk.v263.skript.util.getFirstInSection

/**
 * [SecLoop] を解析して AST のセクションノードを生成するパーサです。
 *
 * 解析結果として該当するノードを含む [AstParseResult] を返します。
 */
object LoopParser: NodeParser<TriggerItem> {
    override val priority: Int = 1000

    override fun canHandle(item: TriggerItem): Boolean = item is SecLoop

    /**
     * 指定した [item] を解析してループのセクションノードを生成します。
     *
     * @param item 解析対象のトリガーアイテム
     * @param context 解析に使用するコンテキスト
     *
     * @return 解析結果の [AstParseResult]
     */
    override fun parse(item: TriggerItem, context: ParseContext<TriggerItem>): AstParseResult {
        val loop = item as SecLoop

        val body = context.parseStruct(loop.getFirstInSection(), loop.next)

        val loopAst = AstNode.Section.Loop(
            handler = loop,
            elements = body.elements
        )

        return AstParseResult.Single(loopAst, loop.actualNext)
    }
}
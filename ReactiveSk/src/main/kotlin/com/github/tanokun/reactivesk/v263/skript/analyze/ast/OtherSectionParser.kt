
package com.github.tanokun.reactivesk.v263.skript.analyze.ast

import ch.njol.skript.lang.Section
import ch.njol.skript.lang.TriggerItem
import com.github.tanokun.reactivesk.compiler.frontend.analyze.ast.AstNode
import com.github.tanokun.reactivesk.skriptadapter.common.analyze.ast.parse.AstParseResult
import com.github.tanokun.reactivesk.skriptadapter.common.analyze.ast.parse.NodeParser
import com.github.tanokun.reactivesk.skriptadapter.common.analyze.ast.parse.ParseContext
import com.github.tanokun.reactivesk.v263.skript.util.getFirstInSection

/**
 * その他のセクションを解析して AST の汎用セクションノードを生成します。
 * 任意の [Section] をハンドルし、その本体を解析して [AstNode.Section.Other] を生成します。
 */
object OtherSectionParser: NodeParser<TriggerItem> {
    override val priority: Int = 0

    override fun canHandle(item: TriggerItem): Boolean = item is Section

    /**
     * 指定した [item] を解析してセクションのノードを生成します。
     *
     * @param item 解析対象のトリガーアイテム
     * @param context 解析に使用するコンテキスト
     *
     * @return 生成された [AstParseResult]
     */
    override fun parse(item: TriggerItem, context: ParseContext<TriggerItem>): AstParseResult {
        val section = item as Section

        val body = context.parseStruct(section.getFirstInSection(), section.next)

        val sectionAst = AstNode.Section.Other(
            handler = section,
            elements = body.elements
        )

        return AstParseResult.Single(sectionAst, section.next)
    }
}
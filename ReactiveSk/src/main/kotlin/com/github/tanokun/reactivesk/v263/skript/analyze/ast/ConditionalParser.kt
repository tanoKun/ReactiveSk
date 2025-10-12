package com.github.tanokun.reactivesk.v263.skript.analyze.ast

import ch.njol.skript.lang.TriggerItem
import ch.njol.skript.sections.SecConditional
import com.github.tanokun.reactivesk.compiler.frontend.analyze.ast.AstNode
import com.github.tanokun.reactivesk.skriptadapter.common.analyze.ast.parse.AstParseResult
import com.github.tanokun.reactivesk.skriptadapter.common.analyze.ast.parse.NodeParser
import com.github.tanokun.reactivesk.skriptadapter.common.analyze.ast.parse.ParseContext
import com.github.tanokun.reactivesk.skriptadapter.common.reflection.Reflection
import com.github.tanokun.reactivesk.v263.skript.util.getFirstInSection

/**
 * 条件分岐構造を解析して [AstNode] のセクションノードを生成するパーサです。
 *
 * IF/ELSE/ELSE_IF の連鎖を解析して対応する AST ノードを構築します。
 */
object ConditionalParser : NodeParser<TriggerItem> {
    override val priority: Int = 1000

    override fun canHandle(item: TriggerItem): Boolean = item is SecConditional

    /**
     * 指定した [item] を解析して条件分岐のセクションノードを生成します。
     *
     * @param item 解析対象のトリガーアイテム
     * @param context 解析に使用するコンテキスト
     *
     * @return 解析結果の [AstParseResult]
     */
    override fun parse(item: TriggerItem, context: ParseContext<TriggerItem>): AstParseResult {
        val conditional = item as SecConditional
        val conditionalType = conditional.conditionalType()

        return if (conditionalType == ConditionalType.IF) {
            val (ifAstNode, endOfChainItem) = parseIfChain(conditional, context)

            AstParseResult.Single(ifAstNode, endOfChainItem)
        } else
            AstParseResult.Skip

    }

    private fun SecConditional.conditionalType(): ConditionalType {
        val field = Reflection.findField(this.javaClass, "type")
        val rawValue = field.get(this) as Enum<*>

        return ConditionalType.valueOf(rawValue.name)
    }

    private fun findIfChain(ifHeader: SecConditional): IfChain {
        val elseIfs = mutableListOf<SecConditional>()
        var elseNode: SecConditional? = null
        var nextLink = ifHeader.normalNext

        while (nextLink is SecConditional) {
            when (nextLink.conditionalType()) {
                ConditionalType.ELSE_IF -> {
                    elseIfs.add(nextLink)
                    nextLink = nextLink.normalNext
                }
                ConditionalType.ELSE -> {
                    elseNode = nextLink
                    break
                }
                ConditionalType.IF -> break
            }
        }

        return IfChain(ifHeader, elseIfs, elseNode)
    }

    private fun parseIfChain(ifHeader: SecConditional, context: ParseContext<TriggerItem>): Pair<AstNode.Section.If<TriggerItem>, TriggerItem?> {
        val chain = findIfChain(ifHeader)

        val thenSection = context.parseStruct(chain.ifHeader.getFirstInSection(), chain.ifHeader.normalNext)

        val elseIfs = chain.elseIfs.map { elseIfNode ->
            AstNode.ElseIf(
                handler = elseIfNode,
                thenSection = context.parseStruct(elseIfNode.getFirstInSection(), elseIfNode.normalNext)
            )
        }

        val elseSection = chain.elseNode?.let {
            context.parseStruct(it.getFirstInSection(), it.normalNext)
        }

        val ifAstNode = AstNode.Section.If(
            handler = chain.ifHeader,
            thenSection = thenSection,
            elseIfSections = elseIfs,
            elseSection = elseSection
        )

        val nextItem = chain.elseNode?.normalNext
            ?: chain.elseIfs.lastOrNull()?.normalNext
            ?: chain.ifHeader.normalNext

        return ifAstNode to nextItem
    }

    private enum class ConditionalType { IF, ELSE_IF, ELSE }

    private data class IfChain(
        val ifHeader: SecConditional,
        val elseIfs: List<SecConditional>,
        val elseNode: SecConditional?
    )
}
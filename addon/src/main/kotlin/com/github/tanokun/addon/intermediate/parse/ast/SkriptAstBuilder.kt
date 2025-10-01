package com.github.tanokun.addon.intermediate.parse.ast

import ch.njol.skript.ScriptLoader
import ch.njol.skript.config.SectionNode
import ch.njol.skript.lang.Section
import ch.njol.skript.lang.TriggerItem
import ch.njol.skript.sections.SecConditional
import com.github.tanokun.addon.analysis.ast.AstSection
import com.github.tanokun.addon.analysis.ast.ElseIf
import com.github.tanokun.addon.analysis.ast.SkriptAst
import com.github.tanokun.addon.intermediate.firstInSection

object SkriptAstBuilder {
    var env: SkriptEnv = DefaultSkriptEnv

    /**
     * SectionNodeからASTを構築するエントリーポイント。
     */
    fun buildFromSectionNode(sectionNode: SectionNode): SkriptAst {
        val items = ScriptLoader.loadItems(sectionNode)
        if (items.isNotEmpty()) {
            // TriggerItemの連結リストを正しく設定
            for (i in 0 until items.size - 1) {
                items[i].setNext(items[i + 1])
            }
        }
        val first = items.firstOrNull()
        return SkriptAst(first, buildFrom(first))
    }

    /**
     * TriggerItemの連結リストの先頭からASTを構築する。
     */
    private fun buildFrom(first: TriggerItem?): AstSection.Block {
        return parseBlock(first, stopExclusive = null)
    }

    /**
     * TriggerItemの連結リストを解析し、BlockのASTを構築するメインロジック。
     * @param startItem 解析を開始するTriggerItem
     * @param stopExclusive このTriggerItemに到達したら解析を終了する（このアイテムは含まない）
     */
    private fun parseBlock(startItem: TriggerItem?, stopExclusive: TriggerItem?): AstSection.Block {
        val elements = mutableListOf<AstSection>()
        var currentItem: TriggerItem? = startItem
        val visitedItems = mutableSetOf<TriggerItem>()

        while (currentItem != null && currentItem !== stopExclusive) {
            if (!visitedItems.add(currentItem)) {
                System.err.println("Warning: Circular reference detected at $currentItem. Stopping parse.")
                break
            }

            val nextItem: TriggerItem?

            when (currentItem) {
                is SecConditional -> {
                    // 直接 currentItem.type に依存せず env を経由する
                    if (env.conditionalType(currentItem) == CType.IF) {
                        val (ifAstNode, endOfChainItem) = parseIfChain(currentItem)
                        elements.add(ifAstNode)
                        nextItem = endOfChainItem
                    } else {
                        elements.add(AstSection.Line(currentItem))
                        nextItem = currentItem.next
                    }
                }

                is Section -> {
                    val sectionBody = parseBlock(currentItem.firstInSection, currentItem.next)
                    elements.add(AstSection.Section(currentItem, sectionBody.elements))

                    // SecLoop などセクション固有の次ノード取得は env に委譲
                    nextItem = env.sectionNext(currentItem)
                }

                else -> {
                    elements.add(AstSection.Line(currentItem))
                    nextItem = currentItem.next
                }
            }
            currentItem = nextItem
        }
        return AstSection.Block(elements)
    }

    /**
     * if文のヘッダーから始まり、後続のelse-if/elseをすべて含むif-chainを解析する。
     * @return 解析して構築したAstSection.Ifノードと、if-chain全体の次に処理すべきTriggerItemのペア
     */
    private fun parseIfChain(ifHeader: SecConditional): Pair<AstSection.If, TriggerItem?> {
        val chain = env.findIfChain(ifHeader)

        val thenSection = parseBlock(chain.ifHeader.firstInSection, chain.ifHeader.normalNext)

        val elseIfs = chain.elseIfs.map { elseIfNode ->
            ElseIf(
                header = elseIfNode,
                thenSection = parseBlock(elseIfNode.firstInSection, elseIfNode.normalNext)
            )
        }

        val elseSection = chain.elseNode?.let {
            parseBlock(it.firstInSection, it.normalNext)
        }

        val ifAstNode = AstSection.If(
            header = chain.ifHeader,
            thenSection = thenSection,
            elseIfSections = elseIfs,
            elseSection = elseSection
        )

        val nextItem = chain.elseNode?.normalNext
            ?: chain.elseIfs.lastOrNull()?.normalNext
            ?: chain.ifHeader.normalNext

        return ifAstNode to nextItem
    }
}

enum class CType { IF, ELSE_IF, ELSE }

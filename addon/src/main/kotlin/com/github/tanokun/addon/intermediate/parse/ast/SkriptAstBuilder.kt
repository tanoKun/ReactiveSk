package com.github.tanokun.addon.intermediate.parse.ast

import ch.njol.skript.ScriptLoader
import ch.njol.skript.config.SectionNode
import ch.njol.skript.lang.Section
import ch.njol.skript.lang.TriggerItem
import ch.njol.skript.sections.SecConditional
import ch.njol.skript.sections.SecLoop
import com.github.tanokun.addon.analysis.ast.AstSection
import com.github.tanokun.addon.analysis.ast.ElseIf
import com.github.tanokun.addon.analysis.ast.SkriptAst
import com.github.tanokun.addon.intermediate.Reflection
import com.github.tanokun.addon.intermediate.firstInSection

object SkriptAstBuilder {

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
        val visitedItems = mutableSetOf<TriggerItem>() // 循環参照対策

        while (currentItem != null && currentItem !== stopExclusive) {
            if (!visitedItems.add(currentItem)) {
                System.err.println("Warning: Circular reference detected at $currentItem. Stopping parse.")
                break
            }

            val nextItem: TriggerItem?

            when (currentItem) {
                is SecConditional -> {
                    if (currentItem.type == CType.IF) {
                        val (ifAstNode, endOfChainItem) = parseIfChain(currentItem)
                        elements.add(ifAstNode)
                        nextItem = endOfChainItem
                    } else {
                        // 単独のelse/else ifは文法エラーだが、ここではLineとして扱う
                        elements.add(AstSection.Line(currentItem))
                        nextItem = currentItem.next
                    }
                }

                is Section -> {
                    // SecLoopを含むすべてのSectionをここで処理
                    val sectionBody = parseBlock(currentItem.firstInSection, currentItem.next)
                    elements.add(AstSection.Section(currentItem, sectionBody.elements))

                    // 次のアイテムの決定ロジックだけを分岐させる
                    nextItem = if (currentItem is SecLoop) {
                        currentItem.actualNext // SecLoopの場合はactualNextを使う
                    } else {
                        currentItem.next // それ以外のSectionはnormalNextを使う
                    }
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
        val chain = findIfChain(ifHeader)

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

        // if-chain全体の後に続くTriggerItemを特定する
        val nextItem = chain.elseNode?.normalNext
            ?: chain.elseIfs.lastOrNull()?.normalNext
            ?: chain.ifHeader.normalNext

        return ifAstNode to nextItem
    }

    /**
     * SecConditional(IF)から始まるif-elseif-elseの連鎖を物理的に収集する。
     */
    private fun findIfChain(ifHeader: SecConditional): IfChain {
        val elseIfs = mutableListOf<SecConditional>()
        var elseNode: SecConditional? = null
        var nextLink = ifHeader.normalNext

        while (nextLink is SecConditional) {
            when (nextLink.type) {
                CType.ELSE_IF -> {
                    elseIfs.add(nextLink)
                    nextLink = nextLink.normalNext
                }
                CType.ELSE -> {
                    elseNode = nextLink
                    break // else でチェーンは終了
                }
                CType.IF -> break // 新しいifが始まったらチェーンは終了
            }
        }
        return IfChain(ifHeader, elseIfs, elseNode)
    }

    private data class IfChain(
        val ifHeader: SecConditional,
        val elseIfs: List<SecConditional>,
        val elseNode: SecConditional?
    )
}


// =================================================================
// 3. Reflection Helpers
// =================================================================

private enum class CType { IF, ELSE_IF, ELSE }

/**
 * SecConditionalの内部的なtypeを取得する拡張プロパティ。
 */
private val SecConditional.type: CType
    get() {
        val field = Reflection.findField(this.javaClass, "type")
        val rawValue = field.get(this) as Enum<*>
        return CType.valueOf(rawValue.name)
    }

fun AstSection.toStructString(indent: String = "", next: String = "    "): String = when (this) {
    is AstSection.Block -> {
        elements.joinToString("\n") { it.toStructString(indent) }
    }
    is AstSection.Line -> {
        "$indent${this.item.toString(null, false)}"
    }
    is AstSection.Section -> buildString {
        appendLine("$indent${this@toStructString.header.toString(null, false)}:")
        // セクションの中身はインデントを1レベル深くする
        this@toStructString.elements.joinToString("\n") { it.toStructString("$indent$next") }
            .takeIf { it.isNotEmpty() }
            ?.let { append(it) }
    }.trimEnd()
    is AstSection.If -> buildString {
        // if節
        appendLine("$indent${this@toStructString.header.toString(null, false)}")
        this@toStructString.thenSection.toStructString("$indent$next").takeIf { it.isNotEmpty() }?.let { appendLine(it) }

        // else if節
        this@toStructString.elseIfSections.forEach { elseIf ->
            appendLine("$indent${elseIf.header.toString(null, false)}")
            elseIf.thenSection.toStructString("$indent$next").takeIf { it.isNotEmpty() }?.let { appendLine(it) }
        }

        // else節
        this@toStructString.elseSection?.let {
            appendLine("${indent}else:")
            it.toStructString("$indent$next").takeIf { it.isNotEmpty() }?.let { append(it) }
        }
    }.trimEnd()
}
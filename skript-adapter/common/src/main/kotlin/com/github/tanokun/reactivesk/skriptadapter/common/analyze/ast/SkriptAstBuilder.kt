package com.github.tanokun.reactivesk.skriptadapter.common.analyze.ast

import ch.njol.skript.config.SectionNode
import ch.njol.skript.lang.TriggerItem
import com.github.tanokun.reactivesk.compiler.frontend.analyze.ast.AstNode
import com.github.tanokun.reactivesk.skriptadapter.common.LoadItemsAdapter
import com.github.tanokun.reactivesk.skriptadapter.common.analyze.ast.parse.AstParseResult
import com.github.tanokun.reactivesk.skriptadapter.common.analyze.ast.parse.ParseContext
import com.github.tanokun.reactivesk.skriptadapter.common.analyze.ast.parse.TriggerItemParser

class SkriptAstBuilder(
    parsers: List<TriggerItemParser>,
    private val loadItemsAdapter: LoadItemsAdapter,
) {
    private val parsers = parsers.sortedByDescending { it.priority }

    fun buildFromSectionNode(sectionNode: SectionNode): SkriptAst {
        val items = loadItemsAdapter.loadFromSectionNode(sectionNode)
        val first = items.firstOrNull()

        return SkriptAst(first, buildFromTriggerItem(first))
    }

    fun buildFromTriggerItem(first: TriggerItem?): AstNode.Struct<TriggerItem> {
        return AstNode.Struct(first, parseBlock(first, stopExclusive = null))
    }

    private fun parseBlock(startItem: TriggerItem?, stopExclusive: TriggerItem?): AstNode.Struct<TriggerItem> {
        val elements = mutableListOf<AstNode<TriggerItem>>()
        var currentItem: TriggerItem? = startItem
        val visitedItems = mutableSetOf<TriggerItem>()

        while (currentItem != null && currentItem !== stopExclusive) {
            if (!visitedItems.add(currentItem)) {
                System.err.println("Warning: Circular reference detected at $currentItem. Stopping parse.")
                break
            }

            val context = ParseContext(
                parseStruct = ::parseBlock,
                stopExclusive = stopExclusive
            )

            val handler = findParser(currentItem)
            val parseResult = handler?.parse(currentItem, context)

            val (astSection, nextItem) = when (parseResult) {
                is AstParseResult.Single -> {
                    parseResult.astNode to parseResult.nextItem
                }
                is AstParseResult.Skip, null -> {
                    AstNode.Line.Other(currentItem) to currentItem.next
                }
            }

            elements.add(astSection)
            currentItem = nextItem
        }

        return AstNode.Struct(startItem, elements)
    }

    private fun findParser(item: TriggerItem): TriggerItemParser? {
        return parsers.firstOrNull { it.canHandle(item) }
    }
}
package com.github.tanokun.reactivesk.skriptadapter.v2_6_3.analyze.ast

import ch.njol.skript.lang.TriggerItem
import ch.njol.skript.sections.SecLoop
import com.github.tanokun.reactivesk.compiler.frontend.analyze.ast.AstNode
import com.github.tanokun.reactivesk.skriptadapter.common.analyze.ast.parse.AstParseResult
import com.github.tanokun.reactivesk.skriptadapter.common.analyze.ast.parse.ParseContext
import com.github.tanokun.reactivesk.skriptadapter.common.analyze.ast.parse.TriggerItemParser
import com.github.tanokun.reactivesk.skriptadapter.v2_6_3.SkriptAdapterV263.getFirstInSection

class LoopParser: TriggerItemParser {
    override val priority: Int = 200

    override fun canHandle(item: TriggerItem): Boolean = item is SecLoop

    override fun parse(item: TriggerItem, context: ParseContext): AstParseResult {
        val loop = item as SecLoop

        val body = context.parseStruct(loop.getFirstInSection(), loop.next)

        val loopAst = AstNode.Section.Loop(
            handler = loop,
            elements = body.elements
        )

        return AstParseResult.Single(loopAst, loop.actualNext)
    }
}
package com.github.tanokun.reactivesk.skriptadapter.v2_6_3.analyze.ast

import ch.njol.skript.lang.Section
import ch.njol.skript.lang.TriggerItem
import com.github.tanokun.reactivesk.compiler.frontend.analyze.ast.AstNode
import com.github.tanokun.reactivesk.skriptadapter.common.analyze.ast.parse.AstParseResult
import com.github.tanokun.reactivesk.skriptadapter.common.analyze.ast.parse.ParseContext
import com.github.tanokun.reactivesk.skriptadapter.common.analyze.ast.parse.TriggerItemParser
import com.github.tanokun.reactivesk.skriptadapter.v2_6_3.SkriptAdapterV263.getFirstInSection

class OtherSectionParser: TriggerItemParser {
    override val priority: Int = 300

    override fun canHandle(item: TriggerItem): Boolean = item is Section

    override fun parse(item: TriggerItem, context: ParseContext): AstParseResult {
        val section = item as Section

        val body = context.parseStruct(section.getFirstInSection(), section.next)

        val sectionAst = AstNode.Section.Other(
            handler = section,
            elements = body.elements
        )

        return AstParseResult.Single(sectionAst, section.next)
    }
}
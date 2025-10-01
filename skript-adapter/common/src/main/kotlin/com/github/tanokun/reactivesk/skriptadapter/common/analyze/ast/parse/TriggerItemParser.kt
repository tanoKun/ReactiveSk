package com.github.tanokun.reactivesk.skriptadapter.common.analyze.ast.parse

import ch.njol.skript.lang.TriggerItem

interface TriggerItemParser {
    val priority: Int

    fun canHandle(item: TriggerItem): Boolean

    fun parse(item: TriggerItem, context: ParseContext): AstParseResult
}
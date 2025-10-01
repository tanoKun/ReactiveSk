package com.github.tanokun.reactivesk.skriptadapter.common.analyze.ast.parse

import ch.njol.skript.lang.TriggerItem
import com.github.tanokun.reactivesk.compiler.frontend.analyze.ast.AstNode

data class ParseContext(
    val parseStruct: (TriggerItem?, TriggerItem?) -> AstNode.Struct<TriggerItem>,
    val stopExclusive: TriggerItem?
)

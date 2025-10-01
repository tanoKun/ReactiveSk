package com.github.tanokun.reactivesk.skriptadapter.common.analyze.ast

import ch.njol.skript.lang.TriggerItem
import com.github.tanokun.reactivesk.compiler.frontend.analyze.ast.AstNode

data class SkriptAst(val first: TriggerItem?, val root: AstNode.Struct<TriggerItem>)
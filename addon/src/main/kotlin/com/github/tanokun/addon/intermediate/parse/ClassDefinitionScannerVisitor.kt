package com.github.tanokun.addon.intermediate.parse

import com.github.tanokun.addon.SkriptClassDefinitionBaseVisitor
import com.github.tanokun.addon.SkriptClassDefinitionParser
import com.github.tanokun.addon.definition.Identifier

class ClassDefinitionScannerVisitor : SkriptClassDefinitionBaseVisitor<List<Identifier>>() {
    override fun visitProgram(ctx: SkriptClassDefinitionParser.ProgramContext): List<Identifier> {
        return ctx.classDef().map { Identifier(it.name.text) }
    }

    override fun aggregateResult(aggregate: List<Identifier>?, nextResult: List<Identifier>?): List<Identifier> {
        return (aggregate ?: emptyList()) + (nextResult ?: emptyList())
    }
}
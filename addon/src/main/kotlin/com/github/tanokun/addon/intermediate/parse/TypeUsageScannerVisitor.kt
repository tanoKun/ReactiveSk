package com.github.tanokun.addon.intermediate.parse

import com.github.tanokun.addon.SkriptClassDefinitionBaseVisitor
import com.github.tanokun.addon.SkriptClassDefinitionParser
import com.github.tanokun.addon.definition.Identifier

/**
 * パースツリーから使用されているすべての型名を抽出するVisitor。
 */
class TypeUsageScannerVisitor : SkriptClassDefinitionBaseVisitor<Unit>() {
    val usedTypes = mutableSetOf<Identifier>()

    override fun visitType(ctx: SkriptClassDefinitionParser.TypeContext) {
        val typeName = if (ctx.ARRAY() != null) ctx.arrayType.text else ctx.typeName.text
        usedTypes.add(Identifier(typeName))
        super.visitType(ctx)
    }
}
package com.github.tanokun.addon.clazz.definition.parse.function

import SkriptClassDefinitionBaseVisitor
import SkriptClassDefinitionLexer
import SkriptClassDefinitionParser
import com.github.tanokun.addon.clazz.definition.Identifier
import com.github.tanokun.addon.clazz.definition.function.FunctionDefinition
import com.github.tanokun.addon.clazz.definition.function.ParameterDefinition

class FunctionSignatureVisitor : SkriptClassDefinitionBaseVisitor<Any>() {
        override fun visitSignature(ctx: SkriptClassDefinitionParser.SignatureContext): Any {
            val name = ctx.IDENTIFIER(0).text

            val parameters = ctx.paramList()?.let { visit(it) as List<ParameterDefinition> } ?: emptyList()
            val returnTypeName = ctx.IDENTIFIER(1)?.text

            return FunctionDefinition(Identifier(name), parameters, returnTypeName)
        }

        override fun visitParamList(ctx: SkriptClassDefinitionParser.ParamListContext): Any {
            return ctx.paramDef().map { visit(it) as ParameterDefinition }
        }

        override fun visitParamDef(ctx: SkriptClassDefinitionParser.ParamDefContext): Any {
            val paramName = ctx.IDENTIFIER(0).text
            val typeName = ctx.IDENTIFIER(1).text
            val isArray = ctx.ARRAY() != null
            return ParameterDefinition(paramName, typeName, isArray)
        }
}
package com.github.tanokun.addon.parse

import com.github.tanokun.addon.SkriptClassDefinitionBaseVisitor
import com.github.tanokun.addon.SkriptClassDefinitionLexer
import com.github.tanokun.addon.SkriptClassDefinitionParser
import com.github.tanokun.addon.definition.dynamic.ClassDefinition
import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.definition.dynamic.constructor.InitSection
import com.github.tanokun.addon.definition.dynamic.constructor.ConstructorParameter
import com.github.tanokun.addon.definition.dynamic.error.ThrowType
import com.github.tanokun.addon.definition.dynamic.field.FieldDefinition
import com.github.tanokun.addon.definition.dynamic.function.FunctionDefinition
import com.github.tanokun.addon.definition.dynamic.function.ParameterDefinition
import java.lang.reflect.Modifier

class DynamicClassParserVisitor : SkriptClassDefinitionBaseVisitor<Any>() {
    data class TypeInfo(val name: Identifier, val isArray: Boolean)

    override fun visitClassDef(ctx: SkriptClassDefinitionParser.ClassDefContext): ClassDefinition {
        val className = Identifier(ctx.name.text)

        val constructorParams = ctx.constructorParams?.constructorParam()
            ?.map { visit(it) as ConstructorParameter } ?: emptyList()

        val initThrows: ArrayList<ThrowType> = arrayListOf()
        val fields = mutableListOf<FieldDefinition>()
        val functions = mutableListOf<FunctionDefinition>()

        ctx.classBody()?.classMember()?.forEach { memberCtx ->
            when {
                memberCtx.functionDef() != null -> functions.add(visitFunctionDef(memberCtx.functionDef()))
                memberCtx.fieldSection() != null -> fields.addAll(visitFieldSection(memberCtx.fieldSection()))
                memberCtx.initSection() != null -> {
                    initThrows.addAll(memberCtx.initSection().throwsList()?.let { visitThrowsList(it) } ?: emptyList())
                }
            }
        }

        val constructorParameterNames = constructorParams.map { it.parameterName }
        val constructorFieldNames = constructorParams.filter { it.isProperty }.map { it.parameterName }

        val constructFields = constructorParams
            .filter(ConstructorParameter::isProperty)
            .map(ConstructorParameter::toFieldDefinition)

        if (constructorParameterNames.toSet().size != constructorParameterNames.size) {
            throw IllegalArgumentException("Duplicate constructor parameter name found in class $className")
        }

        val bodyFieldNames = fields.map { it.fieldName }
        val allFieldNames = constructorFieldNames + bodyFieldNames
        if (allFieldNames.size != allFieldNames.toSet().size) {
            throw IllegalArgumentException("Duplicate field name found in class $className")
        }

        return ClassDefinition(className, constructorParams, fields + constructFields, functions, InitSection(initThrows))
    }

    override fun visitConstructorParam(ctx: SkriptClassDefinitionParser.ConstructorParamContext): ConstructorParameter {
        val modifier = parseModifier(ctx.accessModifiers())

        val isProperty = ctx.mutability != null
        val isMutable = ctx.mutability?.type == SkriptClassDefinitionLexer.VAR

        val paramDef = visit(ctx.arg()) as ParameterDefinition

        return ConstructorParameter(
            parameterName = paramDef.parameterName,
            typeName = paramDef.typeName,
            isArray = paramDef.isArray,
            isProperty = isProperty,
            isMutable = isMutable,
            modifier = modifier
        )
    }

    override fun visitFieldSection(ctx: SkriptClassDefinitionParser.FieldSectionContext): List<FieldDefinition> {
        // field セクションが空でも安全に動作
        return ctx.fieldDef().map { visit(it) as FieldDefinition }
    }

    override fun visitFieldDef(ctx: SkriptClassDefinitionParser.FieldDefContext): FieldDefinition {
        val modifier = parseModifier(ctx.accessModifiers())
        val isMutable = ctx.mutability.type == SkriptClassDefinitionLexer.VAR

        val paramDef = visit(ctx.arg()) as ParameterDefinition

        return FieldDefinition(
            fieldName = paramDef.parameterName,
            typeName = paramDef.typeName,
            isMutable = isMutable,
            isArray = paramDef.isArray,
            modifier = modifier
        )
    }

    private fun parseModifier(ctx: SkriptClassDefinitionParser.AccessModifiersContext?): Int {
        return if (ctx?.PRIVATE() != null) Modifier.PRIVATE else Modifier.PUBLIC
    }

    override fun visitFunctionDef(ctx: SkriptClassDefinitionParser.FunctionDefContext): FunctionDefinition {
        val modifier = parseModifier(ctx.accessModifiers())
        val funcName = Identifier(ctx.name.text)

        val parameters = ctx.funcArgs()?.arg()
            ?.map { visit(it) as ParameterDefinition } ?: emptyList()

        val parameterNames = parameters.map { it.parameterName }
        if (parameterNames.toSet().size != parameterNames.size) {
            throw IllegalArgumentException("Duplicate parameter name found in function $funcName")
        }

        // 戻り型は「:: 型」または「: 型」の両方に対応(functionReturn にラベル付き)
        val returnTypeName: Identifier? =
            ctx.functionReturn()?.returnType?.let { (visit(it) as TypeInfo).name }

        // function 本体はスキップ対象。throws は文法で許容されていれば取得、なければ空
        val throws =
            ctx.throwsList()?.let { visitThrowsList(it) } ?: emptyList()

        return FunctionDefinition(funcName, parameters, returnTypeName, modifier, throws)
    }

    override fun visitArg(ctx: SkriptClassDefinitionParser.ArgContext): ParameterDefinition {
        val name = Identifier(ctx.name.text)
        val typeInfo = visit(ctx.type()) as TypeInfo
        return ParameterDefinition(name, typeInfo.name, typeInfo.isArray)
    }

    override fun visitType(ctx: SkriptClassDefinitionParser.TypeContext): TypeInfo {
        return if (ctx.ARRAY() != null) {
            TypeInfo(Identifier(ctx.arrayType.text), true)
        } else {
            TypeInfo(Identifier(ctx.typeName.text), false)
        }
    }

    override fun visitThrowsList(ctx: SkriptClassDefinitionParser.ThrowsListContext): List<ThrowType> {
        return ctx.throwsParam().map { ThrowType(Identifier(it.throw_.text)) }
    }
}
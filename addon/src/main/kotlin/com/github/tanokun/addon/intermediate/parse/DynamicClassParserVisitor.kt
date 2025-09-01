package com.github.tanokun.addon.intermediate.parse

import com.github.tanokun.addon.SkriptClassDefinitionBaseVisitor
import com.github.tanokun.addon.SkriptClassDefinitionParser
import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.definition.dynamic.ClassDefinition
import com.github.tanokun.addon.definition.dynamic.PropertyModifiers
import com.github.tanokun.addon.definition.dynamic.constructor.ConstructorParameter
import com.github.tanokun.addon.definition.dynamic.constructor.InitSection
import com.github.tanokun.addon.definition.dynamic.error.ThrowType
import com.github.tanokun.addon.definition.dynamic.field.FieldDefinition
import com.github.tanokun.addon.definition.dynamic.function.FunctionDefinition
import com.github.tanokun.addon.definition.dynamic.function.ParameterDefinition
import java.lang.reflect.Modifier

class DynamicClassParserVisitor : SkriptClassDefinitionBaseVisitor<Any>() {
    data class TypeInfo(val name: Identifier, val isArray: Boolean)

    override fun visitClassDef(ctx: SkriptClassDefinitionParser.ClassDefContext): ClassDefinition {
        val className = Identifier(ctx.name.text)

        val constructorParams = ctx.constructorParams
            ?.constructorParam()
            ?.map(::visitConstructorParam) ?: emptyList()

        val initThrows: ArrayList<ThrowType> = arrayListOf()
        val fields = mutableListOf<FieldDefinition>()
        val functions = mutableListOf<FunctionDefinition>()

        ctx.classBody()?.classMember()?.forEach { memberCtx ->
            when {
                memberCtx.functionDef() != null -> functions.add(visitFunctionDef(memberCtx.functionDef()))
                memberCtx.fieldSection() != null -> fields.addAll(visitFieldSection(memberCtx.fieldSection()))
                memberCtx.initSection() != null -> {
                    initThrows.addAll(memberCtx.initSection().throwsList()?.let(::visitThrowsList) ?: emptyList())
                }
            }
        }

        val constructorProperties = constructorParams.filter(ConstructorParameter::isProperty)

        val constructorParameterNames = constructorParams.map(ConstructorParameter::parameterName)
        val constructorFieldNames = constructorProperties.map(ConstructorParameter::parameterName)

        val constructFields = constructorProperties.map(ConstructorParameter::toFieldDefinition)

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
        val modifiers = parseModifier(ctx.accessModifiers())
        val declarationModifiers = visitDeclaration(ctx.declaration())
        val paramDef = visitArg(ctx.arg())

        return ConstructorParameter(
            parameterName = paramDef.parameterName,
            typeName = paramDef.typeName,
            isArray = paramDef.isArray,
            modifiers = modifiers or declarationModifiers,
        )
    }

    override fun visitFieldSection(ctx: SkriptClassDefinitionParser.FieldSectionContext): List<FieldDefinition> {
        return ctx.fieldDef().map { visitFieldDef(it) }
    }

    override fun visitFieldDef(ctx: SkriptClassDefinitionParser.FieldDefContext): FieldDefinition {
        val modifiers = parseModifier(ctx.accessModifiers())
        val declarationModifiers = ctx.declaration()?.let(::visitDeclaration) ?: 0
        val paramDef = visitArg(ctx.arg())

        return FieldDefinition(
            fieldName = paramDef.parameterName,
            typeName = paramDef.typeName,
            isArray = paramDef.isArray,
            modifiers = modifiers or declarationModifiers,
        )
    }

    private fun parseModifier(ctx: SkriptClassDefinitionParser.AccessModifiersContext?): Int {
        return if (ctx?.PRIVATE() != null) Modifier.PRIVATE else Modifier.PUBLIC
    }

    override fun visitFunctionDef(ctx: SkriptClassDefinitionParser.FunctionDefContext): FunctionDefinition {
        val modifier = parseModifier(ctx.accessModifiers())
        val funcName = Identifier(ctx.name.text)

        val parameters = ctx.funcArgs()?.arg()?.map(::visitArg) ?: emptyList()

        val parameterNames = parameters.map { it.parameterName }
        if (parameterNames.toSet().size != parameterNames.size) {
            throw IllegalArgumentException("Duplicate parameter name found in function $funcName")
        }

        val returnTypeName: Identifier? = ctx.functionReturn()?.returnType?.let(::visitType)?.name

        val throws = ctx.throwsList()?.let(::visitThrowsList) ?: emptyList()

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

    override fun visitDeclaration(ctx: SkriptClassDefinitionParser.DeclarationContext?): Int {
        if (ctx == null) return 0

        val isMutable = if (ctx.VAR() != null) PropertyModifiers.MUTABLE else 0
        val isFactor = if (ctx.FACTOR() != null) PropertyModifiers.MUTABLE or PropertyModifiers.FACTOR else 0

        return isMutable or isFactor
    }
}
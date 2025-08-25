package com.github.tanokun.addon.clazz.definition.parse

import com.github.tanokun.addon.SkriptClassDefinitionBaseVisitor
import com.github.tanokun.addon.SkriptClassDefinitionLexer
import com.github.tanokun.addon.SkriptClassDefinitionParser
import com.github.tanokun.addon.clazz.definition.ClassDefinition
import com.github.tanokun.addon.clazz.definition.Identifier
import com.github.tanokun.addon.clazz.definition.field.FieldDefinition
import com.github.tanokun.addon.clazz.definition.function.FunctionDefinition
import com.github.tanokun.addon.clazz.definition.function.ParameterDefinition
import org.antlr.v4.runtime.misc.Interval
import java.lang.reflect.Modifier

class DynamicClassParserVisitor : SkriptClassDefinitionBaseVisitor<Any>() {

    /**
     * Visitor内部での型情報の受け渡しに使うヘルパーデータクラス。
     */
    data class TypeInfo(val name: Identifier, val isArray: Boolean)

    /**
     * 修飾子ノードを `java.lang.reflect.Modifier` の定数に変換するヘルパー関数。
     */
    private fun parseModifier(ctx: SkriptClassDefinitionParser.AccessModifiersContext?): Int {
        return if (ctx?.PRIVATE() != null) Modifier.PRIVATE else Modifier.PUBLIC
    }

    /**
     * クラス定義全体を訪問する。
     */
    override fun visitClassDef(ctx: SkriptClassDefinitionParser.ClassDefContext): ClassDefinition {
        val className = Identifier(ctx.name.text)

        // コンストラクタ引数として定義されたフィールドを処理
        val constructorFields = ctx.classArgs()?.fieldDef()
            ?.map { visit(it) as FieldDefinition } ?: emptyList()

        // クラス本体のメンバー（fieldブロック、functionなど）を処理
        val bodyFields = mutableListOf<FieldDefinition>()
        val functions = mutableListOf<FunctionDefinition>()

        ctx.classBody().classMember().forEach { memberCtx ->
            when {
                memberCtx.functionDef() != null -> functions.add(visit(memberCtx.functionDef()) as FunctionDefinition)
                memberCtx.fieldBlock() != null -> bodyFields.addAll(visit(memberCtx.fieldBlock()) as List<FieldDefinition>)
            }
        }

        val allFields = constructorFields + bodyFields
        return ClassDefinition(className, allFields, functions)
    }

    /**
     * `field:` ブロック全体を訪問する。
     */
    override fun visitFieldBlock(ctx: SkriptClassDefinitionParser.FieldBlockContext): List<FieldDefinition> {
        return ctx.fieldDef().map { visit(it) as FieldDefinition }
    }

    /**
     * 単一のフィールド定義 (`val name: type`) を訪問する。
     */
    override fun visitFieldDef(ctx: SkriptClassDefinitionParser.FieldDefContext): FieldDefinition {
        val modifier = parseModifier(ctx.accessModifiers())
        val isMutable = ctx.mutability.type == SkriptClassDefinitionLexer.VAR

        val paramDef = visit(ctx.arg()) as ParameterDefinition

        return FieldDefinition(
            fieldName = paramDef.name,
            typeName = paramDef.typeName,
            isMutable = isMutable,
            isArray = paramDef.isArray,
            modifier = modifier
        )
    }

    /**
     * 単一の関数定義を訪問する。
     */
    override fun visitFunctionDef(ctx: SkriptClassDefinitionParser.FunctionDefContext): FunctionDefinition {
        val modifier = parseModifier(ctx.accessModifiers())
        val funcName: Identifier = Identifier(ctx.name.text)

        val parameters = ctx.funcArgs()?.arg()
            ?.map { visit(it) as ParameterDefinition } ?: emptyList()

        val returnTypeName: Identifier? = ctx.returnType?.let { (visit(it) as TypeInfo).name }

        val rawBlockText = if (ctx.rawBody() != null) {
            val startIndex = ctx.rawBody().start.startIndex
            val stopIndex = ctx.rawBody().stop.stopIndex
            ctx.start.inputStream.getText(Interval(startIndex, stopIndex)).trim()
        } else {
            ""
        }

        return FunctionDefinition(funcName, parameters, returnTypeName, rawBlockText, modifier)
    }

    /**
     * 引数 (`name: type`) の定義を訪問する。
     */
    override fun visitArg(ctx: SkriptClassDefinitionParser.ArgContext): ParameterDefinition {
        val name: Identifier = Identifier(ctx.name.text)
        val typeInfo = visit(ctx.type()) as TypeInfo
        return ParameterDefinition(name, typeInfo.name, typeInfo.isArray)
    }

    /**
     * 型 (`string` や `array of long`) の定義を訪問する。
     */
    override fun visitType(ctx: SkriptClassDefinitionParser.TypeContext): TypeInfo {
        return if (ctx.ARRAY() != null) {
            TypeInfo(Identifier(ctx.arrayType.text), true)
        } else {
            TypeInfo(Identifier(ctx.typeName.text), false)
        }
    }
}
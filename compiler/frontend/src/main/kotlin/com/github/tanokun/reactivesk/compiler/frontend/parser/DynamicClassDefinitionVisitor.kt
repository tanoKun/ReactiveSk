package com.github.tanokun.reactivesk.compiler.frontend.parser

import com.github.tanokun.reactivesk.compiler.frontend.parser.antlr.DynamicClassDefinitionBaseVisitor
import com.github.tanokun.reactivesk.compiler.frontend.parser.antlr.DynamicClassDefinitionParser.*
import com.github.tanokun.reactivesk.lang.ClassDefinition
import com.github.tanokun.reactivesk.lang.Identifier
import com.github.tanokun.reactivesk.lang.PropertyModifier // Type alias for Int
import com.github.tanokun.reactivesk.lang.PropertyModifiers
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.TerminalNode

/**
 * ANTLRのパースツリーを巡回し、[ClassDefinition] のリストを構築するVisitor。
 *
 * Pythonの文法パターンを参考にしたg4ファイルに合わせて実装されています。
 * 各visitメソッドは具体的なデータモデルの型を返すため、型安全性が高く、不要なキャストを排除しています。
 */
class DynamicClassDefinitionVisitor : DynamicClassDefinitionBaseVisitor<Any>() {

    /**
     * パースツリーのルートから巡回を開始し、ファイル内で定義された全てのクラス定義を抽出します。
     *
     * @param ctx プログラム全体のパースツリーコンテキスト。
     * @return 抽出された [ClassDefinition] のリスト。
     */
    fun visit(ctx: ProgramContext): List<ClassDefinition> =
        visitProgram(ctx)

    override fun visitProgram(ctx: ProgramContext): List<ClassDefinition> =
        ctx.topLevelElement().mapNotNull { it.classDef() }.map { visitClassDef(it) }

    override fun visitClassDef(ctx: ClassDefContext): ClassDefinition {
        val className = ctx.name.toIdentifier()

        val constructorParameters = ctx.constructorParameters?.let { visitConstructorParamList(it) }
            ?: emptyList()

        val classStatements = ctx.classBody()?.classMember() ?: emptyList()

        val initThrows = classStatements
            .mapNotNull { it.initializer()?.throwsList() }
            .flatMap { visitThrowsList(it) }

        val constructor = ClassDefinition.Constructor(constructorParameters, initThrows)

        val uninitializedProperties = classStatements
            .mapNotNull { it.fieldSection() }
            .flatMap { visitFieldSection(it) }

        val functions = classStatements
            .mapNotNull { it.function() }
            .map { visitFunction(it) }

        return ClassDefinition(
            className = className,
            constructor = constructor,
            uninitializedProperty = uninitializedProperties,
            functions = functions
        )
    }

    override fun visitConstructorParamList(ctx: ConstructorParamListContext): List<ClassDefinition.Constructor.Parameter> =
        ctx.constructorParameter().map { visitConstructorParameter(it) }

    override fun visitConstructorParameter(ctx: ConstructorParameterContext): ClassDefinition.Constructor.Parameter =
        ctx.constructorParameterNotProperty()?.let { visitConstructorParameterNotProperty(it) }
            ?: visitConstructorParameterAsProperty(ctx.constructorParameterAsProperty())

    override fun visitConstructorParameterNotProperty(ctx: ConstructorParameterNotPropertyContext): ClassDefinition.Constructor.Parameter.NotProperty {
        val (name, type, isArray) = visitArgument(ctx.argument())
        return ClassDefinition.Constructor.Parameter.NotProperty(name, type, isArray)
    }

    override fun visitConstructorParameterAsProperty(ctx: ConstructorParameterAsPropertyContext): ClassDefinition.Constructor.Parameter.AsInitializedProperty {
        val (name, type, isArray) = visitArgument(ctx.argument())
        val modifiers = visitAccessModifiers(ctx.accessModifiers()) or visitDeclaration(ctx.declaration())
        return ClassDefinition.Constructor.Parameter.AsInitializedProperty(name, type, isArray, modifiers)
    }

    override fun visitFieldSection(ctx: FieldSectionContext): List<ClassDefinition.Property.UninitializedProperty> =
        ctx.field().map { visitField(it) }

    override fun visitField(ctx: FieldContext): ClassDefinition.Property.UninitializedProperty {
        val (name, type, isArray) = visitArgument(ctx.argument())
        val modifiers = visitAccessModifiers(ctx.accessModifiers()) or visitDeclaration(ctx.declaration())
        return ClassDefinition.Property.UninitializedProperty(name, type, isArray, modifiers)
    }

    override fun visitFunction(ctx: FunctionContext): ClassDefinition.Function {
        val functionName = ctx.name.toIdentifier()
        val returns = ctx.functionReturn()?.let { visitFunctionReturn(it) }
            ?: ClassDefinition.Function.Returns(Identifier("void"), false)

        val modifiers = visitAccessModifiers(ctx.accessModifiers())
        val parameters = ctx.functionArguments()?.let { visitFunctionArguments(it) } ?: emptyList()
        val throws = ctx.throwsList()?.let { visitThrowsList(it) } ?: emptyList()

        return ClassDefinition.Function(
            functionName = functionName,
            returns = returns,
            parameters = parameters,
            modifiers = modifiers,
            throws = throws
        )
    }

    override fun visitFunctionArguments(ctx: FunctionArgumentsContext): List<ClassDefinition.Function.Parameter> =
        ctx.argument().map {
            val (name, type, isArray) = visitArgument(it)
            ClassDefinition.Function.Parameter(name, type, isArray)
        }

    override fun visitFunctionReturn(ctx: FunctionReturnContext): ClassDefinition.Function.Returns {
        val (type, isArray) = visitType(ctx.type())
        return ClassDefinition.Function.Returns(type, isArray)
    }

    override fun visitArgument(ctx: ArgumentContext): Triple<Identifier, Identifier, Boolean> {
        val name = ctx.name.toIdentifier()
        val (type, isArray) = visitType(ctx.type())
        return Triple(name, type, isArray)
    }

    override fun visitType(ctx: TypeContext): Pair<Identifier, Boolean> =
        ctx.typeName?.let { it.toIdentifier() to false }
            ?: (ctx.arrayType.toIdentifier() to true)

    override fun visitThrowsList(ctx: ThrowsListContext): List<ClassDefinition.Throws> =
        ctx.throw_().map { visitThrow(it) }

    override fun visitThrow(ctx: ThrowContext): ClassDefinition.Throws =
        ClassDefinition.Throws(ctx.Identifier().toIdentifier())

    override fun visitAccessModifiers(ctx: AccessModifiersContext?): PropertyModifier =
        if (ctx?.PRIVATE() != null) PropertyModifiers.PRIVATE else PropertyModifiers.PUBLIC

    override fun visitDeclaration(ctx: DeclarationContext): PropertyModifier {
        return when {
            ctx.VAR() != null -> PropertyModifiers.MUTABLE
            ctx.FACTOR() != null -> PropertyModifiers.FACTOR or PropertyModifiers.MUTABLE
            ctx.VAL() != null -> 0
            else -> 0
        }
    }

    // --- Helper Extensions ---

    private fun TerminalNode.toIdentifier(): Identifier = Identifier(this.text)

    // Token.toIdentifier() は現在未使用ですが、将来のために残しておきます。
    private fun Token.toIdentifier(): Identifier = Identifier(this.text)
}
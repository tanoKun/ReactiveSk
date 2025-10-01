package com.github.tanokun.reactivesk.compiler.frontend.parser

import com.github.tanokun.reactivesk.compiler.frontend.parser.antlr.DynamicClassDefinitionParser
import com.github.tanokun.reactivesk.lang.ClassDefinition
import com.github.tanokun.reactivesk.lang.PropertyModifiers
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class DynamicClassDefinitionVisitorTest {

    private fun parse(source: String): List<ClassDefinition> {
        val lexer = DynamicClassDefinitionIndentLexer(CharStreams.fromString(source))
        val tokens = CommonTokenStream(lexer)
        val parser = DynamicClassDefinitionParser(tokens)
        val tree = parser.program()

        return DynamicClassDefinitionVisitor().visit(tree)
    }

    @Test
    @DisplayName("field セクションが空の場合")
    fun testEmptyFieldSection() {
        val src = """
        class User:
          field:
    """.trimIndent()
        val result = parse(src)

        assertEquals(1, result.size)
        assertEquals(0, result[0].uninitializedProperty.size)
    }

    @Test
    @DisplayName("デフォルトのアクセス修飾子が PUBLIC であること")
    fun testDefaultAccessModifierIsPublic() {
        val src = """
        class User:
          field:
            val name: string
    """.trimIndent()
        val result = parse(src)
        val prop = result[0].uninitializedProperty[0]

        assertTrue(prop.modifiers and PropertyModifiers.PUBLIC != 0)
    }

    @Test
    @DisplayName("init, field, function のある場合")
    fun testClassWithInitFieldFunction() {
        val src = """
            class User:
              field:
                val name: string
              init:
              function foo():
        """.trimIndent()
        val result = parse(src)
        assertEquals(1, result.size)
        assertEquals(1, result[0].uninitializedProperty.size)
        assertEquals(1, result[0].functions.size)
    }

    @Test
    @DisplayName("modifiers の組み合わせ (factor, mutable, private, public)")
    fun testModifiersCombination() {
        val src = """
            class User:
              field:
                private factor name: string
          """.trimIndent()
        val result = parse(src)
        val prop = result[0].uninitializedProperty[0]
        assertTrue(prop.modifiers and 2 != 0) // PRIVATE
        assertTrue(prop.modifiers and 4 != 0) // FACTOR
        assertTrue(prop.modifiers and 8 != 0) // MUTABLE
    }

    @Test
    @DisplayName("constructor parameter が property である場合とない場合")
    fun testConstructorParameterPropertyAndNot() {
        val src = """
            class User[name: string, private val age: int]:
        """.trimIndent()
        val result = parse(src)
        val params = result[0].constructor.parameters
        assertEquals(2, params.size)
        assertTrue(params[0] is ClassDefinition.Constructor.Parameter.NotProperty)
        assertTrue(params[1] is ClassDefinition.Constructor.Parameter.AsInitializedProperty)
    }

    @Test
    @DisplayName("function の return type が void である場合とない場合")
    fun testFunctionReturnTypeVoidAndNot() {
        val src = """
            class User:
              function foo():
              function bar(): int:
        """.trimIndent()
        val result = parse(src)
        assertEquals("void", result[0].functions[0].returns.typeName.identifier)
        assertEquals("int", result[0].functions[1].returns.typeName.identifier)
    }

    @Test
    @DisplayName("function の throws がある場合とない場合")
    fun testFunctionThrowsPresentAndAbsent() {
        val src = """
            class User:
              function foo() throws [Error]:
              function bar():
        """.trimIndent()
        val result = parse(src)
        assertEquals(1, result[0].functions[0].throws.size)
        assertEquals(0, result[0].functions[1].throws.size)
    }

    @Test
    @DisplayName("class が複数ある場合")
    fun testMultipleClasses() {
        val src = """
            class A:
            class B:
        """.trimIndent()
        val result = parse(src)
        assertEquals(2, result.size)
    }

    @Test
    @DisplayName("class が一つもない場合")
    fun testNoClass() {
        val src = """
            # コメントのみ
        """.trimIndent()
        val result = parse(src)
        assertEquals(0, result.size)
    }

    @Test
    @DisplayName("constructor parameter が一つもない場合")
    fun testNoConstructorParameter() {
        val src = """
            class User:
        """.trimIndent()
        val result = parse(src)
        assertEquals(0, result[0].constructor.parameters.size)
    }

    @Test
    @DisplayName("constructor parameter が複数ある場合")
    fun testMultipleConstructorParameters() {
        val src = """
            class User[name: string, age: int]:
        """.trimIndent()
        val result = parse(src)
        assertEquals(2, result[0].constructor.parameters.size)
    }

    @Test
    @DisplayName("field が複数ある場合")
    fun testMultipleFields() {
        val src = """
            class User:
              field:
                val name: string
                val age: int
          """.trimIndent()
        val result = parse(src)
        assertEquals(2, result[0].uninitializedProperty.size)
    }

    @Test
    @DisplayName("field が一つもない場合")
    fun testNoField() {
        val src = """
            class User:
        """.trimIndent()
        val result = parse(src)
        assertEquals(0, result[0].uninitializedProperty.size)
    }

    @Test
    @DisplayName("function が複数ある場合")
    fun testMultipleFunctions() {
        val src = """
            class User:
              function foo():
              function bar():
        """.trimIndent()
        val result = parse(src)
        assertEquals(2, result[0].functions.size)
    }

    @Test
    @DisplayName("function が一つもない場合")
    fun testNoFunction() {
        val src = """
            class User:
            
        """.trimIndent()
        val result = parse(src)
        assertEquals(0, result[0].functions.size)
    }

    @Test
    @DisplayName("argument の type が array である場合とない場合")
    fun testArgumentTypeArrayAndNot() {
        val src = """
            class User[id: array of int, name: string]:
            
        """.trimIndent()
        val result = parse(src)
        val params = result[0].constructor.parameters
        assertTrue(params[0].isArray)
        assertFalse(params[1].isArray)
    }

    @Test
    @DisplayName("argument が一つもない場合")
    fun testNoArgument() {
        val src = """
            class User:
              function foo():
        """.trimIndent()
        val result = parse(src)
        assertEquals(0, result[0].functions[0].parameters.size)
    }

    @Test
    @DisplayName("argument が複数ある場合")
    fun testMultipleArguments() {
        val src = """
            class User:
              function foo(a: int, b: string):
        """.trimIndent()
        val result = parse(src)

        assertEquals(2, result[0].functions[0].parameters.size)
    }

    @Test
    @DisplayName("throws が複数ある場合")
    fun testMultipleThrows() {
        val src = """
            class User:
              function foo() throws [Error, Exception]:
        """.trimIndent()
        val result = parse(src)
        assertEquals(2, result[0].functions[0].throws.size)
    }

    @Test
    @DisplayName("throws が一つもない場合")
    fun testNoThrows() {
        val src = """
            class User:
              function foo():
        """.trimIndent()
        val result = parse(src)
        assertEquals(0, result[0].functions[0].throws.size)
    }

    @Test
    @DisplayName("function の返り値が array である場合とない場合")
    fun testFunctionReturnArrayAndNot() {
        val src = """
            class User:
              function foo(): array of int:
              function bar(): int: 
        """.trimIndent()
        val result = parse(src)
        assertTrue(result[0].functions[0].returns.isArray)
        assertFalse(result[0].functions[1].returns.isArray)
    }

    @Test
    @DisplayName("セクションだけ定義され空の場合")
    fun testEmptySection() {
        val src = """
            class User:
              init:
        """.trimIndent()
        val result = parse(src)
        // initセクションが空でもエラーにならないこと
        assertEquals(1, result.size)
    }

    @Test
    @DisplayName("通常のSkriptコードやコメントが混在している場合")
    fun testMixedWithSkriptCode() {
        val src = """
            on load:
                broadcast "Hello"

            class PlayerData[name: string]:
              field:
                val score: int

            command /mycmd:
              trigger:
                # do something

            class ServerData:
              field:
                private val motd: string
        """.trimIndent()

        val result = parse(src)

        // class定義だけが2つ正しくパースされていることを確認
        assertEquals(2, result.size)

        // 各クラスの主要な情報が正しいかを確認
        val playerData = result.find { it.className.identifier == "PlayerData" }!!
        assertEquals(1, playerData.constructor.parameters.size)
        assertEquals(1, playerData.uninitializedProperty.size)
        assertEquals("score", playerData.uninitializedProperty[0].propertyName.identifier)

        val serverData = result.find { it.className.identifier == "ServerData" }!!
        assertTrue(serverData.constructor.parameters.isEmpty())
        assertEquals(1, serverData.uninitializedProperty.size)
        assertEquals("motd", serverData.uninitializedProperty[0].propertyName.identifier)
    }

    @Test
    @DisplayName("全ての機能を組み合わせた複雑なクラス")
    fun testComplexKitchenSinkClass() {
        val src = """
            class KitchenSink[name: string, private var uuid: string]:
              field:
                private val creationDate: date
                val scores: array of int

              init throws [Error1, Error2]:
                # some initialization skript code

              function calculateTotal(multiplier: int): string throws [CalculationError]:
                # some function skript code
        """.trimIndent()

        val result = parse(src)
        assertEquals(1, result.size)

        val clazz = result[0]
        assertEquals("KitchenSink", clazz.className.identifier)

        // コンストラクタの検証
        assertEquals(2, clazz.constructor.parameters.size)
        assertTrue(clazz.constructor.parameters[0] is ClassDefinition.Constructor.Parameter.NotProperty)
        assertTrue(clazz.constructor.parameters[1] is ClassDefinition.Constructor.Parameter.AsInitializedProperty)
        assertEquals(2, clazz.constructor.throws.size)
        assertEquals("Error2", clazz.constructor.throws[1].error.identifier)

        // フィールドの検証
        assertEquals(2, clazz.uninitializedProperty.size)
        assertEquals("creationDate", clazz.uninitializedProperty[0].propertyName.identifier)
        assertEquals(false, clazz.uninitializedProperty[0].isArray)
        assertEquals("scores", clazz.uninitializedProperty[1].propertyName.identifier)
        assertEquals(true, clazz.uninitializedProperty[1].isArray)

        // 関数の検証
        assertEquals(1, clazz.functions.size)
        val func = clazz.functions[0]
        assertEquals("calculateTotal", func.functionName.identifier)
        assertEquals("string", func.returns.typeName.identifier) // 仮: returns.typeName
        assertEquals(1, func.parameters.size)
        assertEquals("multiplier", func.parameters[0].parameterName.identifier)
        assertEquals(1, func.throws.size)
        assertEquals("CalculationError", func.throws[0].error.identifier)
    }
}
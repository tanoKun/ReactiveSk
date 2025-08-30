package com.github.tanokun.addon.parse.variable

import ch.njol.skript.config.Node
import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.definition.variable.TypedVariableDeclaration
import com.github.tanokun.addon.definition.variable.TypedVariableResolver
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("TypedVariableResolver の単体テスト")
class TypedVariableResolverTest {

    private fun id(s: String) = Identifier(s)

    @Test
    @DisplayName("同一スコープに存在する宣言を取得できる")
    fun getDeclaration_sameScope() {
        val top = mockk<Node>()
        val decl = TypedVariableDeclaration(id("x"), String::class.java, false, 2)

        TypedVariableResolver.declare(top, decl)

        val result = TypedVariableResolver.getDeclarationInScopeChain(top, 2, id("x"))
        assertNotNull(result)
        assertEquals(decl, result)
    }

    @Test
    @DisplayName("内側スコープの宣言が優先される")
    fun getDeclaration_innerScopePreferred() {
        val top = mockk<Node>()
        val outer = TypedVariableDeclaration(id("x"), Long::class.java, false, 0)
        val inner = TypedVariableDeclaration(id("x"), String::class.java, false, 2)

        TypedVariableResolver.declare(top, outer)
        TypedVariableResolver.declare(top, inner)

        val result = TypedVariableResolver.getDeclarationInScopeChain(top, 2, id("x"))
        assertNotNull(result)
        assertEquals(inner, result, "より内側のスコープの宣言が返るべき")
    }

    @Test
    @DisplayName("外側スコープから可視な宣言を見つける")
    fun getDeclaration_fromOuterScope() {
        val top = mockk<Node>()
        val outer = TypedVariableDeclaration(id("y"), Int::class.javaObjectType, false, 1)

        TypedVariableResolver.declare(top, outer)

        val result = TypedVariableResolver.getDeclarationInScopeChain(top, 3, id("y"))
        assertNotNull(result)
        assertEquals(outer, result)
    }

    @Test
    @DisplayName("名前が一致しない場合は null を返す")
    fun getDeclaration_nameNotFound() {
        val top = mockk<Node>()
        TypedVariableResolver.declare(top, TypedVariableDeclaration(id("y"), String::class.java, false, 1))

        val result = TypedVariableResolver.getDeclarationInScopeChain(top, 1, id("x"))
        assertNull(result)
    }

    @Test
    @DisplayName("トップノードが異なると宣言は見つからない")
    fun getDeclaration_differentTopNode() {
        val topA = mockk<Node>()
        val topB = mockk<Node>()

        TypedVariableResolver.declare(topA, TypedVariableDeclaration(id("x"), String::class.java, false, 1))

        val result = TypedVariableResolver.getDeclarationInScopeChain(topB, 1, id("x"))
        assertNull(result)
    }

    @Test
    @DisplayName("負のスコープを指定した場合は探索されず null")
    fun getDeclaration_negativeScope() {
        val top = mockk<Node>()

        val result = TypedVariableResolver.getDeclarationInScopeChain(top, -1, id("x"))
        assertNull(result)
    }
}

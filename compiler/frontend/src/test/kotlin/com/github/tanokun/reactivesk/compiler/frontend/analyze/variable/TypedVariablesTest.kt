package com.github.tanokun.reactivesk.compiler.frontend.analyze.variable

import com.github.tanokun.reactivesk.lang.Identifier
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class TypedVariablesTest {
    private fun createTypedVariables(): TypedVariables {
        return TypedVariables()
    }

    @Test
    @DisplayName("getDeclaration: 指定した深さとセクションIDで正しい変数宣言を取得できること")
    fun testGetDeclaration() {
        val vars = createTypedVariables()
        val name = Identifier("x")
        val unresolved = TypedVariableDeclaration.Unresolved(name, Any::class.java, false, 1)
        val resolved = unresolved.resolve(0)
        vars.ensureSectionTable(1, 0)[name] = resolved

        val result = vars.getDeclaration(1, 0, name)
        assertEquals(resolved, result)
    }

    @Test
    @DisplayName("getDeclarationInSectionChain: 現在の深さから0までで、最初に見つかった変数宣言を取得できること")
    fun testGetDeclarationInSectionChain_found() {
        val vars = createTypedVariables()
        val name = Identifier("y")
        val unresolved = TypedVariableDeclaration.Unresolved(name, Any::class.java, false, 0)
        val resolved = unresolved.resolve(0)
        vars.ensureSectionTable(0, 0)[name] = resolved

        val result = vars.getDeclarationInSectionChain(2, name)
        assertEquals(resolved, result)
    }

    @Test
    @DisplayName("getDeclarationInSectionChain: 見つからない場合はnullを返すこと")
    fun testGetDeclarationInSectionChain_notFound() {
        val vars = createTypedVariables()
        val name = Identifier("z")
        val result = vars.getDeclarationInSectionChain(2, name)

        assertNull(result)
    }

    @Test
    @DisplayName("getDeclarationInCurrentSectionId: 指定した深さの現在のセクションIDで変数宣言を取得できること")
    fun testGetDeclarationInCurrentSectionId_found() {
        val vars = createTypedVariables()
        val name = Identifier("a")
        val unresolved = TypedVariableDeclaration.Unresolved(name, Any::class.java, false, 1)
        val resolved = unresolved.resolve(0)
        vars.ensureSectionTable(1, 0)[name] = resolved

        val result = vars.getDeclarationInCurrentSectionId(1, name)

        assertEquals(resolved, result)
    }

    @Test
    @DisplayName("getDeclarationInCurrentSectionId: 現在のセクションIDに存在しない場合はnullを返すこと")
    fun testGetDeclarationInCurrentSectionId_notFound() {
        val vars = createTypedVariables()
        val name = Identifier("b")
        vars.setSectionId(1, 1)
        val result = vars.getDeclarationInCurrentSectionId(1, name)

        assertNull(result)
    }

    @Test
    @DisplayName("declare: 変数宣言を登録し、インデックスが正しく設定されること")
    fun testDeclare() {
        val vars = createTypedVariables()
        val name = Identifier("c")
        val unresolved = TypedVariableDeclaration.Unresolved(name, Any::class.java, false, 0)
        val resolved = vars.declare(unresolved)

        assertEquals(0, resolved.index)

        val resolved2 = vars.declare(TypedVariableDeclaration.Unresolved(Identifier("d"), Any::class.java, false, 0))

        assertEquals(1, resolved2.index)
    }

    @Test
    @DisplayName("ensureSectionTable: 同じ深さ・セクションIDで同じマップを返すこと")
    fun ensureSectionTable_creates_and_returns_same_map() {
        val vars = createTypedVariables()
        val map1 = vars.ensureSectionTable(3, 7)
        val map2 = vars.ensureSectionTable(3, 7)

        assertSame(map1, map2)
        assertTrue(map1.isEmpty())
    }

    @Test
    @DisplayName("getSectionId: 初期値は0であること")
    fun getSectionId_default_zero() {
        val vars = createTypedVariables()
        assertEquals(0, vars.getSectionId(42))
    }

    @Test
    @DisplayName("setSectionId: セクションID設定時にテーブルが作成されること")
    fun setSectionId_creates_table() {
        val vars = createTypedVariables()
        vars.setSectionId(2, 5)
        val map = vars.ensureSectionTable(2, 5)
        assertNotNull(map)
        assertTrue(map.isEmpty())
    }

    @Test
    @DisplayName("declare: 同じ名前を別セクションで宣言しても衝突しないこと")
    fun declare_same_name_in_different_sections() {
        val vars = createTypedVariables()
        val name = Identifier("same")
        // declare in section 0
        val r1 = vars.declare(TypedVariableDeclaration.Unresolved(name, Any::class.java, false, 0))
        // switch section
        vars.setSectionId(0, 1)
        val r2 = vars.declare(TypedVariableDeclaration.Unresolved(name, Any::class.java, false, 0))

        assertNotEquals(r1.index, r2.index)
        assertEquals(r1, vars.getDeclaration(0, 0, name))
        assertEquals(r2, vars.getDeclaration(0, 1, name))
    }

    @Test
    @DisplayName("declare: 深いスコープの変数が浅いスコープの変数をシャドウすること")
    fun declare_shadowing_between_depths() {
        val vars = createTypedVariables()
        val name = Identifier("shadow")
        val outer = vars.declare(TypedVariableDeclaration.Unresolved(name, Any::class.java, false, 0))
        val inner = vars.declare(TypedVariableDeclaration.Unresolved(name, Any::class.java, false, 1))

        val foundFromInnerDepth = vars.getDeclarationInSectionChain(1, name)
        val foundFromOuterDepth = vars.getDeclarationInSectionChain(0, name)

        assertEquals(inner, foundFromInnerDepth)
        assertEquals(outer, foundFromOuterDepth)
    }

    @Test
    @DisplayName("declare: 連続宣言で index が連番になること")
    fun index_increment_across_declarations() {
        val vars = createTypedVariables()
        val r0 = vars.declare(TypedVariableDeclaration.Unresolved(Identifier("i0"), Any::class.java, false, 0))
        val r1 = vars.declare(TypedVariableDeclaration.Unresolved(Identifier("i1"), Any::class.java, false, 0))
        val r2 = vars.declare(TypedVariableDeclaration.Unresolved(Identifier("i2"), Any::class.java, false, 1))

        assertEquals(0, r0.index)
        assertEquals(1, r1.index)
        assertEquals(2, r2.index)
    }
}
package com.github.tanokun.addon.definition.variable

import ch.njol.skript.config.Node
import ch.njol.skript.lang.TriggerSection
import com.github.tanokun.addon.definition.Identifier

object TypedVariableResolver {

    // top ノードごとのテーブル: depth -> sectionId -> (name -> declaration)
    private data class Table(
        val byDepth: MutableMap<Int, MutableMap<Int, MutableMap<Identifier, TypedVariableDeclaration>>> = mutableMapOf()
    )

    private val tables = mutableMapOf<Node, Table>()

    private val currentSectionCache = mutableMapOf<Node, MutableMap<Int, Int>>()

    private val lastSectionCache = mutableMapOf<Node, MutableMap<Int, TriggerSection?>>()

    private val indexByTopCache = mutableMapOf<Node, Int>()

    fun touchSection(top: Node, depth: Int, currentSection: TriggerSection?): Int {
        val lastSectionByDepth = lastSectionCache.getOrPut(top) { mutableMapOf() }
        val previousSection = lastSectionByDepth[depth]

        if (previousSection === null) {
            lastSectionByDepth[depth] = currentSection
            val sectionId = getNowSectionId(top, depth)
            ensureSectionTable(top, depth, sectionId)

            return sectionId
        }

        if (previousSection === currentSection) {
            return getNowSectionId(top, depth)
        }

        val next = getNowSectionId(top, depth) + 1
        setNowSectionId(top, depth, next)
        lastSectionByDepth[depth] = currentSection
        return next
    }

    private fun setNowSectionId(top: Node, depth: Int, sectionId: Int) {
        val cache = currentSectionCache.getOrPut(top) { mutableMapOf() }
        cache[depth] = sectionId

        ensureSectionTable(top, depth, sectionId)
    }

    private fun getNowSectionId(top: Node, depth: Int): Int {
        val sectionId = currentSectionCache[top]?.get(depth) ?: 0

        return sectionId
    }

    fun declare(top: Node, declaration: TypedVariableDeclaration): TypedVariableDeclaration {
        val depth = declaration.depth
        val sectionId = getNowSectionId(top, depth)
        val table = ensureSectionTable(top, depth, sectionId)

        val next = indexByTopCache.getOrPut(top) { 0 }
        indexByTopCache[top] = next + 1

        val indexed = declaration.copy(index = next)
        table[declaration.variableName] = indexed

        return indexed
    }

    fun getDeclarationInSingleScope(top: Node, depth: Int, variableName: Identifier): TypedVariableDeclaration? {
        val table = tables[top] ?: return null
        val sid = getNowSectionId(top, depth)

        return table.byDepth[depth]?.get(sid)?.get(variableName)
    }

    fun getDeclarationInScopeChain(top: Node, currentDepth: Int, variableName: Identifier): TypedVariableDeclaration? {
        val table = tables[top] ?: return null
        for (d in currentDepth downTo 0) {
            val sid = getNowSectionId(top, d)
            val found = table.byDepth[d]?.get(sid)?.get(variableName)

            if (found != null) return found
        }

        return null
    }

    fun getIndexInNode(node: Node): Int? = indexByTopCache.getOrDefault(node, 1) - 1

    private fun ensureSectionTable(
        top: Node,
        depth: Int,
        sectionId: Int
    ): MutableMap<Identifier, TypedVariableDeclaration> {
        val t = tables.getOrPut(top) { Table() }
        val depthMap = t.byDepth.getOrPut(depth) { mutableMapOf() }

        return depthMap.getOrPut(sectionId) { mutableMapOf() }
    }
}

package com.github.tanokun.reactivesk.compiler.frontend.analyze.variable

import com.github.tanokun.reactivesk.compiler.frontend.analyze.ast.AstNode
import com.github.tanokun.reactivesk.lang.Identifier

data class Table(
    val byDepth: MutableMap<Depth, MutableMap<SectionId, MutableMap<Identifier, TypedVariableDeclaration.Resolved>>> = mutableMapOf()
)

class TypedVariables(
    private val table: Table = Table(),
    private val sectionIds: MutableMap<Depth, SectionId> = mutableMapOf(),
    private val lastSection: MutableMap<Depth, AstNode.Section<*>?> = mutableMapOf(),
    var index: Int = 0
) {
    fun getSectionId(depth: Depth): SectionId = sectionIds[depth] ?: 0

    fun setSectionId(depth: Depth, sectionId: SectionId) {
        sectionIds[depth] = sectionId

        ensureSectionTable(depth, sectionId)
    }

    fun getLastSection(depth: Depth): AstNode.Section<*>? = lastSection[depth]

    fun setLastSection(depth: Depth, section: AstNode.Section<*>?) {
        lastSection[depth] = section
    }

    fun getDeclaration(depth: Depth, sectionId: SectionId, variableName: Identifier): TypedVariableDeclaration.Resolved? {
        return table.byDepth[depth]?.get(sectionId)?.get(variableName)
    }

    fun getDeclarationInSectionChain(currentDepth: Depth, variableName: Identifier): TypedVariableDeclaration.Resolved? {
        for (depth in currentDepth downTo 0) {
            val sectionId = getSectionId(depth)
            val found = getDeclaration(depth, sectionId, variableName)

            if (found != null) return found
        }
        return null
    }

    fun getDeclarationInCurrentSectionId(depth: Depth, variableName: Identifier): TypedVariableDeclaration.Resolved? {
        val sectionId = getSectionId(depth)

        return getDeclaration(depth, sectionId, variableName)
    }

    fun declare(declaration: TypedVariableDeclaration.Unresolved): TypedVariableDeclaration.Resolved {
        val depth = declaration.depth
        val sectionId = getSectionId(depth)
        val tableMap = ensureSectionTable(depth, sectionId)
        val next = index++

        val indexed = declaration.resolve(next)
        tableMap[declaration.variableName] = indexed

        return indexed
    }

    fun ensureSectionTable(depth: Depth, sectionId: SectionId): MutableMap<Identifier, TypedVariableDeclaration.Resolved> {
        val depthMap = table.byDepth.getOrPut(depth) { mutableMapOf() }

        return depthMap.getOrPut(sectionId) { mutableMapOf() }
    }
}
package com.github.tanokun.reactivesk.compiler.frontend.analyze.variable

import com.github.tanokun.reactivesk.compiler.frontend.analyze.ast.AstNode
import com.github.tanokun.reactivesk.lang.Identifier
import java.util.*

typealias Depth = Int
typealias SectionId = Int

object TypedVariableResolver {
    private val variablesByTop = WeakHashMap<AstNode.Struct<*>, TypedVariables>()

    private fun getOrCreateVariables(top: AstNode.Struct<*>): TypedVariables {
        return variablesByTop.getOrPut(top) {
            TypedVariables()
        }
    }

    fun touchSection(top: AstNode.Struct<*>, depth: Depth, currentSection: AstNode.Section<*>?): Int {
        val vars = getOrCreateVariables(top)
        val previousSection = vars.getLastSection(depth)

        if (previousSection == null) {
            vars.setLastSection(depth, currentSection)
            val sectionId = vars.getSectionId(depth)
            vars.ensureSectionTable(depth, sectionId)
            return sectionId
        }

        if (previousSection === currentSection) return vars.getSectionId(depth)

        val next = vars.getSectionId(depth) + 1
        vars.setSectionId(depth, next)
        vars.setLastSection(depth, currentSection)

        return next
    }

    fun declare(top: AstNode.Struct<*>, declaration: TypedVariableDeclaration.Unresolved): TypedVariableDeclaration.Resolved {
        val vars = getOrCreateVariables(top)
        return vars.declare(declaration)
    }

    fun getDeclarationInSingleScope(top: AstNode.Struct<*>, depth: Int, variableName: Identifier): TypedVariableDeclaration.Resolved? {
        val vars = variablesByTop[top] ?: return null

        return vars.getDeclarationInCurrentSectionId(depth, variableName)
    }

    fun getDeclarationInScopeChain(top: AstNode.Struct<*>, currentDepth: Int, variableName: Identifier): TypedVariableDeclaration.Resolved? {
        val vars = variablesByTop[top] ?: return null

        return vars.getDeclarationInSectionChain(currentDepth, variableName)
    }

    fun getIndexInAstNode(top: AstNode.Struct<*>): Int {
        val vars = variablesByTop[top]

        return vars?.index ?: 0
    }
}
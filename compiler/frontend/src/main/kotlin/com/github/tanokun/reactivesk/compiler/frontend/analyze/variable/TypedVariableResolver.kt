package com.github.tanokun.reactivesk.compiler.frontend.analyze.variable

import com.github.tanokun.reactivesk.compiler.frontend.analyze.ast.AstNode
import com.github.tanokun.reactivesk.lang.Identifier

typealias Depth = Int
typealias SectionId = Int

object TypedVariableResolver {
    private val variablesByTop = mutableMapOf<AstNode.Root, TypedVariables>()

    private fun getOrCreateVariables(top: AstNode.Root): TypedVariables {
        return variablesByTop.getOrPut(top) {
            TypedVariables()
        }
    }

    fun touchSection(top: AstNode.Root, depth: Depth, currentSection: AstNode.Section?): Int {
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

    fun declare(top: AstNode.Root, declaration: TypedVariableDeclaration.Unresolved): TypedVariableDeclaration.Resolved {
        val vars = getOrCreateVariables(top)
        return vars.declare(declaration)
    }

    fun getDeclarationInSingleScope(top: AstNode.Root, depth: Int, variableName: Identifier): TypedVariableDeclaration.Resolved? {
        val vars = variablesByTop[top] ?: return null

        return vars.getDeclarationInCurrentSectionId(depth, variableName)
    }

    fun getDeclarationInScopeChain(top: AstNode.Root, currentDepth: Int, variableName: Identifier): TypedVariableDeclaration.Resolved? {
        val vars = variablesByTop[top] ?: return null

        return vars.getDeclarationInSectionChain(currentDepth, variableName)
    }

    fun getIndexInAstNode(top: AstNode.Root): Int {
        val vars = variablesByTop[top]

        return vars?.index ?: 0
    }
}
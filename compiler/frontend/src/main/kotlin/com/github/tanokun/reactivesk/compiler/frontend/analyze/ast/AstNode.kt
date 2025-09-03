package com.github.tanokun.reactivesk.compiler.frontend.analyze.ast


sealed interface AstNode {
    val lineNumber: Int
    val line: String

    data class ElseIf(val lineNumber: Int, val line: String, val thenSection: Block)

    sealed interface Section : AstNode {
        data class Loop(override val lineNumber: Int, override val line: String, val elements: List<AstNode>) : Section
        data class Other(override val lineNumber: Int, override val line: String, val elements: List<AstNode>) : Section
        data class If(
            override val lineNumber: Int, override val line: String,
            val thenSection: Block, val elseIfSections: List<ElseIf>, val elseSection: Block?
        ) : Section
    }

    data class Block(override val lineNumber: Int, override val line: String, val elements: List<AstNode>) : AstNode

    sealed interface Line: AstNode {
        data class FunReturn(override val lineNumber: Int, override val line: String) : Line
        data class Other(override val lineNumber: Int, override val line: String) : Line
    }

    data class Root(override val lineNumber: Int, override val line: String) : AstNode
}
package com.github.tanokun.reactivesk.compiler.frontend.analyze.ast

import com.github.tanokun.reactivesk.lang.Identifier

sealed interface AstNode<H> {
    val handler: H?

    data class ElseIf<H>(val handler: H?, val thenSection: Struct<H>)

    sealed interface Section<H> : AstNode<H> {
        data class Loop<H>(override val handler: H?, val elements: List<AstNode<H>>) : Section<H>
        data class Other<H>(override val handler: H?, val elements: List<AstNode<H>>) : Section<H>
        data class If<H>(
            override val handler: H?,
            val thenSection: Struct<H>, val elseIfSections: List<ElseIf<H>>, val elseSection: Struct<H>?
        ) : Section<H>
    }

    data class Struct<H>(override val handler: H?, val elements: List<AstNode<H>>) : AstNode<H>

    sealed interface Line<H>: AstNode<H> {
        data class FunReturn<H>(override val handler: H?) : Line<H>
        data class ResolveField<H>(override val handler: H?, val fieldName: Identifier) : Line<H>
        data class Exit<H>(override val handler: H?) : Line<H>
        data class Other<H>(override val handler: H?) : Line<H>
    }
}
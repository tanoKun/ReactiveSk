package com.github.tanokun.reactivesk.v263.skript.analyze

import ch.njol.skript.Skript
import ch.njol.skript.lang.parser.ParserInstance
import com.github.tanokun.reactivesk.compiler.frontend.analyze.ast.AbstractDataFlowAnalyzer
import com.github.tanokun.reactivesk.compiler.frontend.analyze.ast.AstNode
import com.github.tanokun.reactivesk.compiler.frontend.analyze.ast.result.Severity
import com.github.tanokun.reactivesk.v263.skript.runtime.invoke.instantiation.ResolveFieldValueEffect

fun AbstractDataFlowAnalyzer.AnalysisResult<*, *>.printlnToSkript(parser: ParserInstance? = null) {
    val diagnostics = this.diagnostics

    diagnostics.forEach {
        val location = it.location
        if (location is AstNode.Line.ResolveField && parser != null) {
            parser.node = (location.handler as ResolveFieldValueEffect).parseNode
        }

        when (it.severity) {
            Severity.ERROR -> Skript.error(it.message)
            Severity.WARNING -> Skript.warning(it.message)
        }
    }
}
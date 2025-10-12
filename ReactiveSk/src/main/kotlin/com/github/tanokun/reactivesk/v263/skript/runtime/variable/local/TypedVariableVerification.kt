package com.github.tanokun.reactivesk.v263.skript.runtime.variable.local

import ch.njol.skript.Skript
import ch.njol.skript.config.Node
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.TriggerSection
import ch.njol.skript.lang.parser.ParserInstance
import com.github.tanokun.reactivesk.compiler.frontend.analyze.variable.TypedVariableDeclaration
import com.github.tanokun.reactivesk.compiler.frontend.analyze.variable.TypedVariableResolver
import com.github.tanokun.reactivesk.lang.Identifier
import com.github.tanokun.reactivesk.v263.skript.util.getDepth
import com.github.tanokun.reactivesk.v263.skript.util.getTopNode

@Suppress("UNCHECKED_CAST")
fun verifyAndTouchTypedVariable(
    identifierExpr: Expression<*>?, parser: ParserInstance, typedVariableResolver: TypedVariableResolver<Node, TriggerSection>
): TypedVariableDeclaration.Resolved? {
    val variableName = (identifierExpr as? Expression<Identifier>)?.getSingle(null) ?: run {
        Skript.error("Variable name $identifierExpr is not invalid.")
        return null
    }

    val node = parser.node ?: run {
        Skript.error("Cannot find node.")
        return null
    }

    val topNode = node.getTopNode()
    val depth = node.getDepth()

    val currentSection = parser.currentSections.lastOrNull()

    typedVariableResolver.touchSection(topNode, depth, currentSection)

    return typedVariableResolver.getDeclarationInScopeChain(topNode, depth, variableName) ?: run {
        Skript.error("Typed variable '$variableName' is not declared in scope chain.")
        return null
    }
}
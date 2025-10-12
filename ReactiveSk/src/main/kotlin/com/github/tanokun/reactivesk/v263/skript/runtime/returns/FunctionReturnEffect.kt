package com.github.tanokun.reactivesk.v263.skript.runtime.returns

import ch.njol.skript.Skript
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.TriggerItem
import ch.njol.skript.sections.SecLoop
import ch.njol.skript.sections.SecWhile
import ch.njol.skript.util.LiteralUtils
import ch.njol.util.Kleenean
import com.github.tanokun.reactivesk.v263.skript.resolve.clazz.FunctionDefinitionInjectorSection
import com.github.tanokun.reactivesk.v263.skript.runtime.invoke.function.mediator.RuntimeFunctionMediator
import com.github.tanokun.reactivesk.v263.skript.util.PriorityRegistration
import org.bukkit.event.Event

@Suppress("UNCHECKED_CAST")
class FunctionReturnEffect : Effect() {
    companion object {
        fun register() {
            PriorityRegistration.register<FunctionReturnEffect>("return %object%")
        }
    }

    private lateinit var valueExpr: Expression<Any>

    private lateinit var returnType: Class<*>

    override fun init(
        exprs: Array<out Expression<*>>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult,
    ): Boolean {
        val injector = parser.currentSections
            .filterIsInstance<FunctionDefinitionInjectorSection>()
            .firstOrNull() ?: let { return false }

        valueExpr = LiteralUtils.defendExpression(exprs[0])
        returnType = injector.method.returnType

        if (returnType == Void.TYPE) {
            Skript.error("Cannot use 'fun return' in function '$returnType' that returns void.")
            return false
        }

        if (!valueExpr.isSingle) {
            Skript.error("Definition $valueExpr must be single.")
            return false
        }

        if (!returnType.isAssignableFrom(valueExpr.returnType)) {
            Skript.error("Cannot return $valueExpr because it's not type '${returnType.simpleName}' but '${valueExpr.returnType.simpleName}'.")
            return false
        }

        return true
    }

    override fun walk(e: Event): TriggerItem? {
        if (e !is RuntimeFunctionMediator) throw IllegalArgumentException("Not a RuntimeFunctionMediator: '$e'")

        val value = if (valueExpr.isSingle) valueExpr.getSingle(e) else valueExpr.getArray(e)
        e.setReturn(value)

        var parent = getParent()

        while (parent != null) {
            if (parent is SecLoop) parent.exit(e)
            else if (parent is SecWhile) parent.reset()

            parent = parent.getParent()
        }

        return null
    }

    override fun toString(e: Event?, debug: Boolean): String = "fun return $valueExpr"

    override fun execute(e: Event?) = throw UnsupportedOperationException("Cannot execute FunctionReturnEffect directly.")
}
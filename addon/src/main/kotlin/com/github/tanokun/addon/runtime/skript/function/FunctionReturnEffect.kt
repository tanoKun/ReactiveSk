package com.github.tanokun.addon.runtime.skript.function

import ch.njol.skript.Skript
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.TriggerItem
import ch.njol.skript.sections.SecLoop
import ch.njol.skript.sections.SecWhile
import ch.njol.skript.util.LiteralUtils
import ch.njol.util.Kleenean
import com.github.tanokun.addon.definition.skript.dynamic.FunctionDefinitionInjector
import com.github.tanokun.addon.intermediate.metadata.MethodMetadata
import com.github.tanokun.addon.runtime.skript.function.call.mediator.RuntimeFunctionMediator
import org.bukkit.event.Event

@Suppress("UNCHECKED_CAST")
class FunctionReturnEffect : Effect() {

    companion object {
        init {
            Skript.registerEffect(FunctionReturnEffect::class.java, "fun return %object%")
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
        val injector = parser.currentSections.firstOrNull { it is FunctionDefinitionInjector } as? FunctionDefinitionInjector ?: let {
            Skript.error("Cannot use 'fun return' outside of function.")
            return false
        }

        valueExpr = LiteralUtils.defendExpression(exprs[0])
        returnType = injector.method.getAnnotation(MethodMetadata::class.java).returnType.java

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

    override fun execute(e: Event?) { }
}
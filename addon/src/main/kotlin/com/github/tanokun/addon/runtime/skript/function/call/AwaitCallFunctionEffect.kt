/*
package com.github.tanokun.addon.runtime.skript.function.call

import ch.njol.skript.Skript
import ch.njol.skript.lang.*
import ch.njol.skript.util.LiteralUtils
import ch.njol.util.Kleenean
import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.definition.skript.dynamic.ClassDefinitionSkriptEvent
import com.github.tanokun.addon.definition.dynamic.DynamicClass
import org.bukkit.event.Event
import java.lang.IllegalArgumentException

@Suppress("UNCHECKED_CAST")
class AwaitCallFunctionEffect : Effect() {

    companion object {
        init {
            Skript.registerEffect(
                AwaitCallFunctionEffect::class.java,
                "await %object%.%identifier%\\([%-objects%]\\) [on %~objects%]"
            )
        }
    }

    private lateinit var funcName: String
    private lateinit var targetExpr: Expression<Any>
    private lateinit var argsExpr: Array<Expression<Any>>
    private var variableExpr: Expression<Variable<*>>? = null

    private var inClass: Class<out DynamicClass>? = null

    override fun init(
        exprs: Array<out Expression<*>?>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult,
    ): Boolean {
        targetExpr = exprs[0] as Expression<Any>
        funcName = (exprs[1] as Expression<Identifier>).getSingle(null)?.identifier ?: let {
            Skript.error("関数名は、リテラルにしてください。 ${exprs[1]}")
            return false
        }

        variableExpr = exprs.getOrNull(2) as? Expression<Variable<*>>
        inClass = (parser.currentSkriptEvent as? ClassDefinitionSkriptEvent)?.dynamicClass

        val argsExpr: Expression<Any> = LiteralUtils.defendExpression(exprs[2] ?: ExpressionList(arrayOf(), Any::class.java, false))
        this.argsExpr =
            if (argsExpr is ExpressionList<*>)
                (argsExpr as ExpressionList<Any>).expressions as Array<Expression<Any>>
            else arrayOf(argsExpr)

        return true
    }

    override fun walk(e: Event): TriggerItem? {
        val target = targetExpr.getSingle(e) ?: return next
        val method = target::class.java.methods.find { it.name == funcName } ?: let {
            throw IllegalArgumentException("存在しない関数名です。 '${funcName}'")
        }

        if (inClass == target::class.java) method.isAccessible = true

       //method.invoke(target, *argsExpr.map { it.getSingle(e) }.toTypedArray())

*/
/*        call(funcNameExpr, targetExpr, argsExpr, e) { function, target, arguments ->
            val awaitFunction = AwaitFunctionRuntimeBukkitEvent()
            val localVars = Variables.removeLocals(e)

            coroutineScope.launch {
                val returnValue = function.call(target, arguments, awaitFunction)
                val value = returnValue as? Array<*> ?: arrayOf(returnValue)

                if (localVars != null) Variables.setLocalVariables(e, localVars)
                variableExpr?.change(e, value, Changer.ChangeMode.SET)

                var timing: Any? = null
                if (SkriptTimings.enabled()) {
                    val trigger = getTrigger()
                    if (trigger != null) {
                        timing = SkriptTimings.start(trigger.debugLabel)
                    }
                }

                walk(next, e)
                Variables.removeLocals(e)

                SkriptTimings.stop(timing)
            }

            return null
        }*//*


        return next
    }
    override fun execute(e: Event) { throw UnsupportedOperationException() }

    override fun toString(e: Event?, debug: Boolean): String? = "await call"
}
*/

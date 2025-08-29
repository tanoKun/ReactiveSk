package com.github.tanokun.addon.runtime.skript.array

import ch.njol.skript.Skript
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionType
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.skript.registrations.Classes
import ch.njol.skript.util.LiteralUtils
import ch.njol.util.Kleenean
import com.github.tanokun.addon.definition.Identifier
import org.bukkit.event.Event
import java.util.ArrayList

@Suppress("UNCHECKED_CAST")
class TransformSingleTypeArrayExpression : SimpleExpression<ArrayList<*>>() {

    private lateinit var objectsExpr: Expression<Any?>

    private lateinit var className: String

    companion object {
        init {
            Skript.registerExpression(
                TransformSingleTypeArrayExpression::class.java,
                ArrayList::class.java,
                ExpressionType.COMBINED,
                "transform %-objects% to single type of %identifier% array"
            )
        }
    }

    override fun init(
        exprs: Array<out Expression<*>>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult
    ): Boolean {
        objectsExpr = LiteralUtils.defendExpression<Any?>(exprs[0]) as Expression<Any?>
        className = (exprs[1] as Expression<Identifier>).getSingle(null)?.identifier ?: let {
            Skript.error("ClassInfo is null. '${exprs[1]}'")
            return false
        }

        return true
    }

    override fun get(e: Event): Array<out ArrayList<Any?>> {
        val targetClazz = Classes.getClass(className)

        val srcValues: Array<Any?> = (objectsExpr.getArray(e) as? Array<Any?>) ?: emptyArray()

        val out = ArrayList<Any?>(srcValues.size)
        for (v in srcValues) {
            if (v != null && targetClazz.isInstance(v)) {
                out.add(v)
            }
        }
        return arrayOf(out)
    }

    override fun toString(e: Event?, debug: Boolean): String = "transform $objectsExpr to single type of $className array"

    override fun isSingle(): Boolean = true

    override fun getReturnType(): Class<out ArrayList<Any?>> =
        ArrayList::class.java as Class<out ArrayList<Any?>>
}
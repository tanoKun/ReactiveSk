package com.github.tanokun.addon.runtime.skript.array

import ch.njol.skript.Skript
import ch.njol.skript.classes.ClassInfo
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionType
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.skript.util.LiteralUtils
import ch.njol.util.Kleenean
import com.github.tanokun.addon.runtime.skript.SkriptExpressionInitChecks.checkSingletonError
import org.bukkit.event.Event

@Suppress("UNCHECKED_CAST")
class TransformSingleTypeArrayExpression : SimpleExpression<ArrayList<*>>() {

    private lateinit var objectsExpr: Expression<Any?>

    private lateinit var classInfo: ClassInfo<*>

    companion object {
        init {
            Skript.registerExpression(
                TransformSingleTypeArrayExpression::class.java,
                ArrayList::class.java,
                ExpressionType.COMBINED,
                "transform %-objects% to single type of %*classinfo% array"
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

        if (checkSingletonError(exprs[1])) return false
        classInfo = (exprs[1] as Expression<ClassInfo<*>>).getSingle(null) ?: return false

        return true
    }

    override fun get(e: Event): Array<out ArrayList<Any?>> {
        val targetClazz = classInfo.c

        val srcValues: Array<Any?> = (objectsExpr.getArray(e) as? Array<Any?>) ?: emptyArray()

        val out = ArrayList<Any?>(srcValues.size)
        for (v in srcValues) {
            if (v != null && targetClazz.isInstance(v)) {
                out.add(v)
            }
        }
        return arrayOf(out)
    }

    override fun toString(e: Event?, debug: Boolean): String = "transform $objectsExpr to single type of ${classInfo.c.simpleName} array"

    override fun isSingle(): Boolean = true

    override fun getReturnType(): Class<out ArrayList<Any?>> =
        ArrayList::class.java as Class<out ArrayList<Any?>>
}
package com.github.tanokun.addon.instance.create

import ch.njol.skript.Skript
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.ExpressionList
import ch.njol.skript.lang.ExpressionType
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.util.SimpleExpression
import ch.njol.skript.util.LiteralUtils
import ch.njol.util.Kleenean
import com.github.tanokun.addon.clazz.ClassRegistry
import com.github.tanokun.addon.clazz.definition.Identifier
import com.github.tanokun.addon.clazz.loader.StaticClassDefinitionLoader
import com.github.tanokun.addon.instance.AnyInstance
import com.sun.org.apache.xml.internal.serializer.utils.Utils.messages
import org.bukkit.event.Event

class NewInstanceExpression: SimpleExpression<AnyInstance>() {
    companion object {
        init {
            val classes = StaticClassDefinitionLoader.loadedClasses
                .map { it.simpleName }

            Skript.registerExpression(
                NewInstanceExpression::class.java,
                AnyInstance::class.java,
                ExpressionType.SIMPLE,
                "create (${classes.reduceOrNull { acc, className -> "$acc|$className" }}) [with %objects%]"
            )
        }
    }

    private lateinit var className: Identifier
    private lateinit var valueExprs: Array<Expression<Any>>

    override fun init(
        exprs: Array<out Expression<*>>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult,
    ): Boolean {
        val valueExprs: Expression<Any> = LiteralUtils.defendExpression(exprs[0])
        this.valueExprs =
            if (valueExprs is ExpressionList<*>)
                (valueExprs as ExpressionList<Any>).getExpressions() as Array<Expression<Any>>
            else arrayOf(valueExprs)

        className = Identifier(parseResult.expr.split(" ", limit = 3)[1])

        return true
    }

    override fun get(e: Event): Array<AnyInstance> {
        val instance = runCatching { arrayOf(ClassRegistry.newInstance(className, valueExprs, e)) }
            .getOrElse {
                Skript.warning("インスタンスの生成に失敗しました: ${it.message} $className")
                throw it
            }

        return instance
    }

    override fun isSingle(): Boolean = true

    override fun getReturnType() = AnyInstance::class.java

    override fun toString(e: Event?, debug: Boolean): String {
        return "new instance with id $className and value $valueExprs"
    }
}
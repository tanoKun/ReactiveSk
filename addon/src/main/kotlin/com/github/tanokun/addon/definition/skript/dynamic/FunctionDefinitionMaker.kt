package com.github.tanokun.addon.definition.skript.dynamic

import ch.njol.skript.ScriptLoader
import ch.njol.skript.Skript
import ch.njol.skript.config.SectionNode
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.Section
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.TriggerItem
import ch.njol.util.Kleenean
import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.dynamicJavaClassLoader
import com.github.tanokun.addon.intermediate.generator.internalFunctionTriggerFieldOf
import com.github.tanokun.addon.definition.variable.TypedVariableDeclaration
import com.github.tanokun.addon.definition.variable.TypedVariableResolver
import com.github.tanokun.addon.definition.skript.variable.getScopeCount
import com.github.tanokun.addon.definition.skript.variable.getTopNode
import org.bukkit.event.Event

/**
 * class セクション内で function セクションを宣言し、Skript の TriggerItem 連結と格納を行います。
 */
class FunctionDefinitionMaker : Section() {

    companion object {
        init {
            Skript.registerSection(FunctionDefinitionMaker::class.java, "function <.+>")
        }
    }

    lateinit var thisDynamicClass: Class<*>
        private set

    lateinit var functionName: String
        private set

    override fun init(
        exprs: Array<out Expression<*>>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult,
        sectionNode: SectionNode,
        triggerItems: List<TriggerItem>,
    ): Boolean {
        val classDefinitionMaker = parser.currentSkriptEvent as? ClassDefinitionSkriptEvent ?: let {
            Skript.error("'function' definition can only be used inside a 'class' section.")
            return false
        }

        functionName = parseResult.expr.split(" ", limit = 3)[1].split("(", limit = 2)[0]
        thisDynamicClass = classDefinitionMaker.dynamicClass ?: return true
        val functionDefinition = classDefinitionMaker.dynamicClassDefinition?.functions?.firstOrNull { it.name.identifier == functionName } ?: let {
            Skript.error("Cannnot find function '$functionName' in 'class' section.")
            return false
        }

        val scopeCount = sectionNode.getScopeCount()
        val topNode = sectionNode.getTopNode()

        TypedVariableResolver.addDeclaration(topNode, TypedVariableDeclaration(Identifier("this"), thisDynamicClass, false, scopeCount))

        functionDefinition.parameters.forEach { param ->
            val variableName = param.parameterName
            val resolvedType = dynamicJavaClassLoader.getClassOrListOrNullFromAll(param.typeName, param.isArray) ?: let {
                Skript.error("Cannot resolve type '${param.typeName}' for '$variableName' in function '${functionDefinition.name}'.")
                return false
            }

            TypedVariableResolver.addDeclaration(topNode, TypedVariableDeclaration(variableName, resolvedType, false, scopeCount))
        }

        parser.currentSections.add(this)

        val items = ScriptLoader.loadItems(sectionNode)

        if (items.size > 1) {
            for (i in 0 until items.size - 1) {
                items[i].setNext(items[i + 1])
            }

            thisDynamicClass.getField(internalFunctionTriggerFieldOf(functionName)).set(null, items[0])
        }

        parser.currentSections.remove(this)

        return true
    }

    override fun walk(e: Event?): TriggerItem? { return null }

    override fun toString(e: Event?, debug: Boolean): String = "function definition section"
}
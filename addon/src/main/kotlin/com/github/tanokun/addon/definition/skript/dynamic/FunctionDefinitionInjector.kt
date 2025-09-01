package com.github.tanokun.addon.definition.skript.dynamic

import ch.njol.skript.Skript
import ch.njol.skript.config.SectionNode
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.Section
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.TriggerItem
import ch.njol.skript.registrations.Classes
import ch.njol.util.Kleenean
import com.github.tanokun.addon.analysis.ast.AstSection
import com.github.tanokun.addon.analysis.ast.result.Severity.ERROR
import com.github.tanokun.addon.analysis.ast.result.Severity.WARNING
import com.github.tanokun.addon.analysis.section.LocalTypedVariableCapacityAnalyzer
import com.github.tanokun.addon.analysis.section.function.FunctionReturnAnalyzer
import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.definition.variable.TypedVariableDeclaration
import com.github.tanokun.addon.definition.variable.TypedVariableResolver
import com.github.tanokun.addon.definition.variable.getDepth
import com.github.tanokun.addon.definition.variable.getTopNode
import com.github.tanokun.addon.intermediate.generator.internalFunctionLocalsCapacityFieldOf
import com.github.tanokun.addon.intermediate.generator.internalFunctionNameOf
import com.github.tanokun.addon.intermediate.generator.internalFunctionTriggerField
import com.github.tanokun.addon.intermediate.parse.ast.SkriptAstBuilder
import com.github.tanokun.addon.moduleManager
import com.github.tanokun.addon.runtime.skript.init.ResolveTypedValueFieldEffect
import org.bukkit.event.Event
import java.lang.reflect.Method

/**
 * class セクション内で function セクションを宣言し、Skript の TriggerItem 連結と格納を行います。
 */
class FunctionDefinitionInjector : Section() {

    companion object {
        init {
            Skript.registerSection(FunctionDefinitionInjector::class.java,
                "function <.+>",
                "private function <.+>"
            )
        }
    }

    lateinit var thisDynamicClass: Class<*>
        private set

    lateinit var method: Method

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

        val functionName = parseResult.expr.split(" ", limit = 3)[matchedPattern + 1].split("(", limit = 2)[0]
        val thisClassDefinition = classDefinitionMaker.dynamicClassDefinition ?: return false
        thisDynamicClass = classDefinitionMaker.dynamicClass ?: return true

        val functionDefinition = classDefinitionMaker.dynamicClassDefinition?.functions?.firstOrNull { it.name.identifier == functionName } ?: let {
            Skript.error("Cannot find function '$functionName' in 'class' section.")
            return false
        }

        method = thisDynamicClass.methods.first { it.name == internalFunctionNameOf(functionName) }

        val depth = sectionNode.getDepth()
        val topNode = sectionNode.getTopNode()

        TypedVariableResolver.declare(topNode, TypedVariableDeclaration(Identifier("this"), thisDynamicClass, false, depth))

        functionDefinition.parameters.forEach { param ->
            val variableName = param.parameterName
            val resolvedType = moduleManager.getLoadedClass(param.typeName) ?: Classes.getClassInfoNoError(param.typeName.identifier)?.c ?: let {
                Skript.error("Cannot resolve type '${param.typeName}' for '$variableName' in function '${functionDefinition.name}'.")
                return false
            }

            TypedVariableResolver.declare(topNode, TypedVariableDeclaration(variableName, resolvedType, false, depth))
        }

        parser.currentSections.add(this)

        val (triggerItem, astRoot) = SkriptAstBuilder.buildFromSectionNode(sectionNode)
        thisDynamicClass.getField(internalFunctionTriggerField(functionName)).set(null, triggerItem)

        val returnAnalyzer = FunctionReturnAnalyzer(astRoot, Identifier(functionName), thisClassDefinition.className, functionDefinition.returnTypeName != null)
        val (problems, _) = returnAnalyzer.analyze()

        problems.forEach {
            val triggerItem = (it.location as? AstSection.Line)?.item
            val originNode = parser.node
            if (triggerItem is ResolveTypedValueFieldEffect) parser.node = triggerItem.parseNode

            when (it.severity) {
                ERROR -> Skript.error(it.message)
                WARNING -> Skript.warning(it.message)
            }

            parser.node = originNode
        }

        val localsAnalyzer = LocalTypedVariableCapacityAnalyzer(astRoot)
        val (_, localCapacity) = localsAnalyzer.analyze()

        thisDynamicClass.getField(internalFunctionLocalsCapacityFieldOf(functionName)).set(null, localCapacity + 1 + functionDefinition.parameters.size)

        parser.currentSections.remove(this)

        return true
    }

    override fun walk(e: Event?): TriggerItem? { return null }

    override fun toString(e: Event?, debug: Boolean): String = "function definition section"
}
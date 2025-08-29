package com.github.tanokun.addon.definition.skript.dynamic

import ch.njol.skript.Skript
import ch.njol.skript.config.SectionNode
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.Section
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.TriggerItem
import ch.njol.util.Kleenean
import com.github.tanokun.addon.analysis.ast.AstSection
import com.github.tanokun.addon.analysis.ast.result.Severity.*
import com.github.tanokun.addon.analysis.init.InitSectionAnalyzer
import com.github.tanokun.addon.definition.Identifier
import com.github.tanokun.addon.definition.dynamic.ClassDefinition
import com.github.tanokun.addon.dynamicJavaClassLoader
import com.github.tanokun.addon.intermediate.generator.INTERNAL_INIT_TRIGGER_SECTION
import com.github.tanokun.addon.parse.SkriptAstBuilder
import com.github.tanokun.addon.parse.toStructString
import com.github.tanokun.addon.definition.variable.TypedVariableDeclaration
import com.github.tanokun.addon.definition.variable.TypedVariableResolver
import com.github.tanokun.addon.definition.skript.variable.getScopeCount
import com.github.tanokun.addon.definition.skript.variable.getTopNode
import com.github.tanokun.addon.definition.dynamic.DynamicClass
import com.github.tanokun.addon.definition.dynamic.constructor.ConstructorParameter
import com.github.tanokun.addon.runtime.skript.init.ResolveTypedValueFieldEffect
import org.bukkit.event.Event

class InitDefinitionInjector: Section() {
    companion object {
        init {
            Skript.registerSection(InitDefinitionInjector::class.java, "init [<.+>]")
        }
    }

    lateinit var thisClassDefinition: ClassDefinition
        private set

    lateinit var thisDynamicClass: Class<out DynamicClass>
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
            Skript.error("'init' definition can only be used inside a 'class' section.")
            return false
        }

        thisClassDefinition = classDefinitionMaker.dynamicClassDefinition ?: return false
        thisDynamicClass = classDefinitionMaker.dynamicClass ?: return false

        val scopeCount = sectionNode.getScopeCount()
        val topNode = sectionNode.getTopNode()

        TypedVariableResolver.addDeclaration(topNode, TypedVariableDeclaration(Identifier("this"), thisDynamicClass, false, scopeCount))

        thisClassDefinition.constructorParameters.forEach { param ->
            val variableName = param.parameterName
            val resolvedType = dynamicJavaClassLoader.getClassOrListOrNullFromAll(param.typeName, param.isArray) ?: let {
                Skript.error("Cannot resolve type '${param.typeName}' for '$variableName' in class '${thisClassDefinition.className}' init .")
                return false
            }

            TypedVariableResolver.addDeclaration(topNode, TypedVariableDeclaration(variableName, resolvedType, false, scopeCount))
        }

        parser.currentSections.add(this)

        val (triggerItem, astRoot) = SkriptAstBuilder.buildFromSectionNode(sectionNode)
        thisDynamicClass.getField(INTERNAL_INIT_TRIGGER_SECTION).set(null, triggerItem)

        val analyzer = InitSectionAnalyzer(astRoot, thisClassDefinition.className, thisClassDefinition.getRequiredInitializationFields())
        val problems = analyzer.analyze()

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

        parser.currentSections.remove(this)

        return true
    }

    override fun walk(e: Event?): TriggerItem? { return null }

    override fun toString(e: Event?, debug: Boolean): String? = "init definition injector"

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = javaClass.hashCode()
}
package com.github.tanokun.reactivesk.skriptadapter.v2_6_3.skript.resolve

import ch.njol.skript.Skript
import ch.njol.skript.config.SectionNode
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.Section
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.TriggerItem
import ch.njol.util.Kleenean
import com.github.tanokun.addon.intermediate.generator.CONSTRUCTOR_LOCALS_CAPACITY
import com.github.tanokun.reactivesk.compiler.backend.codegen.util.CONSTRUCTOR_TRIGGER_SECTION
import com.github.tanokun.reactivesk.compiler.frontend.analyze.ConstructorAnalyzer
import com.github.tanokun.reactivesk.compiler.frontend.analyze.variable.TypedVariableDeclaration
import com.github.tanokun.reactivesk.compiler.frontend.analyze.variable.TypedVariableResolver
import com.github.tanokun.reactivesk.lang.Identifier
import com.github.tanokun.reactivesk.skriptadapter.common.analyze.ast.SkriptAstBuilder
import com.github.tanokun.reactivesk.skriptadapter.common.dynamic.DynamicManager
import com.github.tanokun.reactivesk.skriptadapter.common.skript.CurrentClassParserPatch.currentResolvingClass
import com.github.tanokun.reactivesk.skriptadapter.common.skript.getDepth
import com.github.tanokun.reactivesk.skriptadapter.common.skript.getTopNode
import org.bukkit.event.Event
import org.koin.java.KoinJavaComponent.inject

class ConstructorResolverEffect: Section() {
    companion object {
        init {
            Skript.registerSection(ConstructorResolverEffect::class.java, "init [<.+>]")
        }
    }

    private val astBuilder: SkriptAstBuilder by inject(SkriptAstBuilder::class.java)

    private val dynamicManager: DynamicManager by inject(DynamicManager::class.java)

    override fun init(
        exprs: Array<out Expression<*>>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult,
        sectionNode: SectionNode,
        triggerItems: List<TriggerItem>,
    ): Boolean {
        val resolvingClass = parser.currentResolvingClass ?: let {
            Skript.error("'init' definition can only be used inside a 'class' section.")
            return false
        }

        val depth = sectionNode.getDepth()
        val topNode = sectionNode.getTopNode()

        val definition = resolvingClass.definition
        val clazz = resolvingClass.clazz

        TypedVariableResolver.declare(topNode, TypedVariableDeclaration.Unresolved(Identifier("this"), clazz, false, depth))

        definition.constructor.parameters.forEach { param ->
            val variableName = param.parameterName
            val resolvedType = dynamicManager.getLoadedClass(param.typeName) ?: let {
                Skript.error("Cannot resolve type '${param.typeName}' for '$variableName' in class '${definition.className}' init.")
                return false
            }

            TypedVariableResolver.declare(topNode, TypedVariableDeclaration.Unresolved(variableName, resolvedType, false, depth))
        }

        parser.currentSections.add(this)

        val (triggerItem, astRoot) = astBuilder.buildFromSectionNode(sectionNode)
        clazz.getField(CONSTRUCTOR_TRIGGER_SECTION).set(null, triggerItem)

        val analyzer = ConstructorAnalyzer(astRoot, definition.className, definition.uninitializedProperty)
        analyzeSection(analyzer, parser)

        thisDynamicClass.getField(CONSTRUCTOR_LOCALS_CAPACITY).set(null, localCapacity + 1 + definition.constructor.parameters.size)

        parser.currentSections.remove(this)


        return true
    }

    override fun walk(e: Event?): TriggerItem? { return null }

    override fun toString(e: Event?, debug: Boolean): String = "init definition injector"

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = javaClass.hashCode()
}
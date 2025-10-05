package com.github.tanokun.reactivesk.v263.skript.resolve.clazz

import ch.njol.skript.Skript
import ch.njol.skript.config.SectionNode
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.Section
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.TriggerItem
import ch.njol.util.Kleenean
import com.github.tanokun.reactivesk.compiler.backend.codegen.util.CONSTRUCTOR_TRIGGER_SECTION
import com.github.tanokun.reactivesk.compiler.frontend.analyze.ConstructorAnalyzer
import com.github.tanokun.reactivesk.compiler.frontend.analyze.variable.TypedVariableDeclaration
import com.github.tanokun.reactivesk.lang.Identifier
import com.github.tanokun.reactivesk.skriptadapter.common.analyze.ast.SkriptAstBuilder
import com.github.tanokun.reactivesk.v263.ReactiveSkAddon
import com.github.tanokun.reactivesk.v263.ReactiveSkAddon.Companion.dynamicManager
import com.github.tanokun.reactivesk.v263.skript.analyze.printlnToSkript
import com.github.tanokun.reactivesk.v263.skript.resolve.patch.CurrentClassParserPatch.currentResolvingClass
import com.github.tanokun.reactivesk.v263.skript.resolve.patch.ResolvingClass
import com.github.tanokun.reactivesk.v263.skript.util.ReflectionClassesBySkript.getClassBySkript
import com.github.tanokun.reactivesk.v263.skript.util.getDepth
import com.github.tanokun.reactivesk.v263.skript.util.getTopNode
import org.bukkit.event.Event

class ConstructorInjectorSection: Section() {
    companion object {
        fun register() {
            Skript.registerSection(ConstructorInjectorSection::class.java, "init [<.+>]")
        }
    }

    private val typedVariableResolver = ReactiveSkAddon.typedVariableResolver

    private val astBuilder: SkriptAstBuilder<SectionNode, TriggerItem> = ReactiveSkAddon.skriptAstBuilder

    lateinit var resolvingClass: ResolvingClass private set

    override fun init(
        exprs: Array<out Expression<*>>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult,
        sectionNode: SectionNode,
        triggerItems: List<TriggerItem>,
    ): Boolean {
        resolvingClass = parser.currentResolvingClass ?: let {
            Skript.error("'init' definition can only be used inside a 'class' section.")
            return false
        }

        val depth = sectionNode.getDepth()
        val topNode = sectionNode.getTopNode()

        val definition = resolvingClass.definition
        val clazz = resolvingClass.clazz

        typedVariableResolver.declare(topNode, TypedVariableDeclaration.Unresolved(Identifier("this"), clazz, false, depth))

        definition.constructor.parameters.forEach { param ->
            val variableName = param.parameterName
            val resolvedType = dynamicManager.getLoadedClass(param.typeName) ?: getClassBySkript(param.typeName.identifier) ?: let {
                Skript.error("Cannot resolve type '${param.typeName}' for '$variableName' in class '${definition.className}' init.")
                return false
            }

            typedVariableResolver.declare(topNode, TypedVariableDeclaration.Unresolved(variableName, resolvedType, false, depth))
        }

        parser.currentSections.add(this)

        val (triggerItem, astRoot) = astBuilder.buildFromSectionNode(sectionNode)
        clazz.getField(CONSTRUCTOR_TRIGGER_SECTION).set(null, triggerItem)

        ConstructorAnalyzer(astRoot, definition.className, definition.uninitializedProperty)
            .analyze()
            .printlnToSkript(parser)

        parser.currentSections.remove(this)

        return true
    }

    override fun walk(e: Event?): TriggerItem? { return null }

    override fun toString(e: Event?, debug: Boolean): String = "init definition injector"

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = javaClass.hashCode()
}
package com.github.tanokun.reactivesk.v263.skript.resolve.clazz

import ch.njol.skript.Skript
import ch.njol.skript.config.SectionNode
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.Section
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.TriggerItem
import ch.njol.util.Kleenean
import com.github.tanokun.reactivesk.compiler.backend.codegen.method.TriggerMetadata
import com.github.tanokun.reactivesk.compiler.backend.codegen.util.internalFunctionTriggerField
import com.github.tanokun.reactivesk.compiler.frontend.analyze.FunctionReturnAnalyzer
import com.github.tanokun.reactivesk.compiler.frontend.analyze.variable.TypedVariableDeclaration
import com.github.tanokun.reactivesk.lang.Identifier
import com.github.tanokun.reactivesk.v263.ReactiveSkAddon
import com.github.tanokun.reactivesk.v263.skript.analyze.printlnToSkript
import com.github.tanokun.reactivesk.v263.skript.resolve.patch.CurrentClassParserPatch.currentResolvingClass
import com.github.tanokun.reactivesk.v263.skript.util.ReflectionClassesBySkript.getClassBySkript
import com.github.tanokun.reactivesk.v263.skript.util.getDepth
import com.github.tanokun.reactivesk.v263.skript.util.getTopNode
import org.bukkit.event.Event
import java.lang.reflect.Method
import java.util.*

private val methodIndexes = WeakHashMap<Class<*>, Int>()

/**
 * class セクション内で function セクションを宣言し、Skript の TriggerItem 連結と格納を行います。
 */
class FunctionDefinitionInjectorSection : Section() {
    companion object {
        fun register() {
            Skript.registerSection(FunctionDefinitionInjectorSection::class.java,
                "function <.+>",
                "private function <.+>"
            )
        }
    }

    private val typedVariableResolver = ReactiveSkAddon.typedVariableResolver

    private val dynamicManager = ReactiveSkAddon.dynamicManager

    private val astBuilder = ReactiveSkAddon.skriptAstBuilder

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
        val resolvingClass = parser.currentResolvingClass ?: let {
            Skript.error("'function' definition can only be used inside a 'class' section.")
            return false
        }

        val functionName = parseResult.expr.split(" ", limit = 3)[matchedPattern + 1].split("(", limit = 2)[0]
        thisDynamicClass = resolvingClass.clazz

        val methodIndex = methodIndexes.getOrPut(thisDynamicClass) { 0 }

        if (methodIndex + 1 == resolvingClass.definition.functions.size)
            methodIndexes.remove(thisDynamicClass)
        else
            methodIndexes[thisDynamicClass] = methodIndex + 1

        val functionDefinition = resolvingClass.definition.functions
            .getOrNull(methodIndex)
            ?: let {
                Skript.error("Cannot find function '$functionName' in 'class' section.")
                return false
            }

        method = thisDynamicClass.methods
            .filter { it.annotations.any { anno -> anno is TriggerMetadata } }
            .first {
                it.getAnnotation(TriggerMetadata::class.java).triggerField == internalFunctionTriggerField(functionName, methodIndex)
            }

        val topNode = sectionNode.getTopNode()
        val depth = sectionNode.getDepth()

        typedVariableResolver.declare(
            top = topNode,
            declaration = TypedVariableDeclaration.Unresolved(Identifier("this"), thisDynamicClass, false, depth)
        )

        functionDefinition.parameters.forEach { param ->
            val variableName = param.parameterName
            
            val resolvedType = dynamicManager.getLoadedClass(param.typeName) ?: getClassBySkript(param.typeName.identifier) ?: let {
                Skript.error("Cannot resolve type '${param.typeName}' for '$variableName' in function '${functionDefinition.functionName}'.")
                return false
            }

            typedVariableResolver.declare(
                top = topNode,
                declaration = TypedVariableDeclaration.Unresolved(variableName, resolvedType, false, depth)
            )
        }

        parser.currentSections.add(this)

        val (triggerItem, astRoot) = astBuilder.buildFromSectionNode(sectionNode)

        val triggerMetaAnnotation = method.getAnnotation(TriggerMetadata::class.java)
        thisDynamicClass.getField(triggerMetaAnnotation.triggerField).set(null, triggerItem)

        FunctionReturnAnalyzer(
            functionBodyAst = astRoot,
            functionName = Identifier(functionName),
            className = resolvingClass.definition.className,
            hasReturnValue = functionDefinition.returns.typeName != Identifier("void")
        )
            .analyze()
            .printlnToSkript()

        parser.currentSections.remove(this)

        return true
    }

    override fun walk(e: Event?): TriggerItem? { return null }

    override fun toString(e: Event?, debug: Boolean): String = "function definition section"
}
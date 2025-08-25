package com.github.tanokun.addon.clazz.definition.parse.function.register

import ch.njol.skript.ScriptLoader
import ch.njol.skript.Skript
import ch.njol.skript.classes.ClassInfo
import ch.njol.skript.config.SectionNode
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.Section
import ch.njol.skript.lang.SkriptParser
import ch.njol.skript.lang.Trigger
import ch.njol.skript.lang.TriggerItem
import ch.njol.skript.log.ErrorQuality
import ch.njol.skript.registrations.Classes
import ch.njol.util.Kleenean
import com.github.tanokun.addon.clazz.definition.function.FunctionDefinition
import com.github.tanokun.addon.clazz.definition.function.ParameterDefinition
import com.github.tanokun.addon.clazz.definition.parse.currentlyCombinator
import com.github.tanokun.addon.clazz.definition.parse.function.FunctionParser
import com.github.tanokun.addon.clazz.definition.parse.function.ParseFunctionResult
import com.github.tanokun.addon.instance.call.FunctionReturnEffect
import com.github.tanokun.addon.maker.ClassDefinitionBukkitEvent
import com.google.common.reflect.Parameter
import jdk.nashorn.internal.codegen.CompilerConstants.className
import jdk.nashorn.internal.objects.NativeFunction.function
import org.bukkit.WorldCreator.name
import org.bukkit.event.Event

class FunctionDefinitionRegister : Section() {

    companion object {
        init {
            Skript.registerSection(FunctionDefinitionRegister::class.java, "function <.+>")
        }
    }

    override fun init(
        exprs: Array<out Expression<*>>,
        matchedPattern: Int,
        isDelayed: Kleenean,
        parseResult: SkriptParser.ParseResult,
        sectionNode: SectionNode,
        triggerItems: List<TriggerItem>,
    ): Boolean {
        val currentEvents = parser.currentEvents
        if (currentEvents == null || currentEvents.none { it == ClassDefinitionBukkitEvent::class.java }) {
            Skript.error("'function' 定義は 'class' ブロックの内部でのみ使用できます。", ErrorQuality.SEMANTIC_ERROR)
            return false
        }

        val currentlyCombinator = currentlyCombinator ?: return false
        val className = currentlyCombinator.className

        val signatureLine = sectionNode.key ?: ""
        val signature = signatureLine.substring("function ".length)

        val antlrFuncParser = FunctionParser()
        var function: FunctionDefinition? = null
        when (val result = antlrFuncParser.parseSignature(signature)) {
            is ParseFunctionResult.Success -> {
                val functionDefinition = result.definition
                val verifyParameterError =
                    verifyParameterTypes(functionDefinition, className.identifier)

                FunctionReturnEffect.currentlyReturn = verifyReturnType(functionDefinition, className.identifier)

                val items = ScriptLoader.loadItems(sectionNode)

                if (items.size > 1) {
                    for (i in 0 until items.size - 1) {
                        items[i].setNext(items[i + 1])
                    }
                }

                result.definition.apply {
                    this.trigger = Trigger(parser.currentScript?.file, "function ${this.name}", null, items)
                }

                if (!verifyParameterError.any { it }) function = functionDefinition
            }

            is ParseFunctionResult.Failure -> {
                Skript.error("クラス '$className' の関数 '${signature}' の定義にエラーがあります: ${result.errorMessage}", ErrorQuality.SEMANTIC_ERROR)
                true
            }
        }

        function?.let { currentlyCombinator.addFunction(it) }

        return function != null
    }

    fun verifyParameterTypes(function: FunctionDefinition, className: String): List<Boolean> {
        val parameters = function.parameters

        val result = parameters.map {
            val classInfo = Classes.getClassInfoNoError(it.typeName.lowercase())
            if (classInfo == null) {
                Skript.error("クラス '$className' の関数パラメータの '${function.name}' の型 '${it.typeName}' は存在しません。", ErrorQuality.SEMANTIC_ERROR)
                return@map true
            }

            return@map false
        }

        return result
    }

    fun verifyReturnType(function: FunctionDefinition, className: String): ClassInfo<*>? {
        val returnTypeName = function.returnTypeName ?: return null

        val classInfo = Classes.getClassInfoNoError(returnTypeName.lowercase())
        if (classInfo == null) {
            Skript.error("クラス '$className' の関数返り値の '${function.name}' の型 '${returnTypeName}' は存在しません。", ErrorQuality.SEMANTIC_ERROR)
        }

        return classInfo
    }

    override fun walk(e: Event?): TriggerItem? { return null }

    override fun toString(e: Event?, debug: Boolean): String = "function definition section"
}
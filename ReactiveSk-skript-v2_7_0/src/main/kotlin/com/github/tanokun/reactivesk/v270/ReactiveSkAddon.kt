package com.github.tanokun.reactivesk.v270

import ch.njol.skript.Skript
import ch.njol.skript.SkriptAddon
import com.github.tanokun.reactivesk.v263.ClassesRegister
import com.github.tanokun.reactivesk.v263.skript.analyze.ast.*
import com.github.tanokun.reactivesk.v263.skript.resolve.variable.LocalTypedVariableDeclarationEffect
import com.github.tanokun.reactivesk.v263.skript.runtime.array.TransformSingleTypeArrayExpression
import com.github.tanokun.reactivesk.v263.skript.runtime.function.FunctionReturnEffect
import com.github.tanokun.reactivesk.v263.skript.runtime.function.call.NonSuspendCallFunctionEffect
import com.github.tanokun.reactivesk.v263.skript.runtime.function.call.NonSuspendCallFunctionExpression
import com.github.tanokun.reactivesk.v263.skript.runtime.instantiation.InstantiationExpression
import com.github.tanokun.reactivesk.v263.skript.runtime.instantiation.ResolveFieldValueEffect
import com.github.tanokun.reactivesk.v263.skript.runtime.observe.ObserverSkriptEvent
import com.github.tanokun.reactivesk.v263.skript.runtime.variable.CastExpression
import com.github.tanokun.reactivesk.v263.skript.runtime.variable.field.GetTypedValueFieldExpression
import com.github.tanokun.reactivesk.v263.skript.runtime.variable.field.SetTypedValueFieldEffect
import com.github.tanokun.reactivesk.v263.skript.runtime.variable.local.GetLocalTypedVariableExpression
import com.github.tanokun.reactivesk.v263.skript.runtime.variable.local.SetLocalTypedVariableEffect
import org.bukkit.plugin.java.JavaPlugin

typealias ReactiveSkAddonV263 = com.github.tanokun.reactivesk.v263.ReactiveSkAddon

class ReactiveSkAddon : JavaPlugin() {
    companion object {
        lateinit var plugin: ReactiveSkAddon private set

        val classResolver get() = ReactiveSkAddonV263.classResolver

        val bytecodeGenerator get() = ReactiveSkAddonV263.bytecodeGenerator

        val dynamicManager get() = ReactiveSkAddonV263.dynamicManager

        val definitionLoader get() = ReactiveSkAddonV263.dynamicManager.definitionLoader

        val skriptAstBuilder get() = ReactiveSkAddonV263.skriptAstBuilder

        val typedVariableResolver get() = ReactiveSkAddonV263.typedVariableResolver

        val methodCallers get() = ReactiveSkAddonV263.methodCallers

        val constructorCallers get() = ReactiveSkAddonV263.constructorCallers

        val coroutineScope get() = ReactiveSkAddonV263.coroutineScope
    }

    lateinit var addon: SkriptAddon
        private set

    @Suppress("UNCHECKED_CAST")
    override fun onEnable() {
        plugin = this

        ClassesRegister.registerAll()

        dynamicManager.reload().forEach {
            logger.info("Loaded class: $it")
        }

        ReactiveSkAddonV263.registerClass(dataFolder)
        ReactiveSkAddonV263.registerClassToSkript(logger)

        addon = Skript.registerAddon(this)
        registerToSkript()

        logger.info("ReactiveSk Addon has been enabled successfully!")
    }

    private fun registerToSkript() {
        // Class TODO()

        // Function
        FunctionReturnEffect.register()
        NonSuspendCallFunctionEffect.register()
        NonSuspendCallFunctionExpression.register()

        // Variable
        LocalTypedVariableDeclarationEffect.register()
        GetLocalTypedVariableExpression.register()
        SetLocalTypedVariableEffect.register()

        // Field
        GetTypedValueFieldExpression.register()
        SetTypedValueFieldEffect.register()

        // Instance
        InstantiationExpression.register()
        ResolveFieldValueEffect.register()

        // Observe
        ObserverSkriptEvent.register()

        // Util
        CastExpression.register()
        TransformSingleTypeArrayExpression.register()

    }

    override fun onDisable() {
        logger.info("ReactiveSk Addon has been disabled.")
    }
}

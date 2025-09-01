package com.github.tanokun.addon.runtime

import ch.njol.skript.Skript
import ch.njol.skript.lang.parser.ParserInstance
import com.github.tanokun.addon.definition.dynamic.PropertyModifier
import com.github.tanokun.addon.definition.dynamic.PropertyModifiers.isPrivate
import com.github.tanokun.addon.definition.skript.dynamic.FunctionDefinitionInjector
import com.github.tanokun.addon.definition.skript.dynamic.InitDefinitionInjector

object DynamicAccessChecks {
    fun Class<*>.checkAccessError(parser: ParserInstance, modifier: PropertyModifier, error: String): Boolean {
        if (modifier.isPrivate() && !isInSelf(parser)) {
            Skript.error(error)
            return true
        }

        return false
    }

    private fun Class<*>.isInSelf(parser: ParserInstance): Boolean {
        parser.getCurrentSections(InitDefinitionInjector::class.java).firstOrNull()?.let {
            if (it.thisDynamicClass == this) return true
        }

        parser.getCurrentSections(FunctionDefinitionInjector::class.java).firstOrNull()?.let {
            if (it.thisDynamicClass == this) return true
        }

        return false
    }
}
package com.github.tanokun.reactivesk.v263.skript.runtime

import ch.njol.skript.Skript
import ch.njol.skript.lang.parser.ParserInstance
import com.github.tanokun.reactivesk.lang.PropertyModifier
import com.github.tanokun.reactivesk.lang.PropertyModifiers.isPrivate
import com.github.tanokun.reactivesk.v263.skript.resolve.clazz.ConstructorInjectorSection
import com.github.tanokun.reactivesk.v263.skript.resolve.clazz.FunctionDefinitionInjectorSection

object DynamicAccessChecks {
    fun Class<*>.checkAccessError(parser: ParserInstance, modifier: PropertyModifier, error: String): Boolean {
        if (modifier.isPrivate() && !isInSelf(parser)) {
            Skript.error(error)
            return true
        }

        return false
    }

    private fun Class<*>.isInSelf(parser: ParserInstance): Boolean {
        parser.getCurrentSections(ConstructorInjectorSection::class.java).firstOrNull()?.let {
            if (it.resolvingClass.clazz == this) return true
        }

        parser.getCurrentSections(FunctionDefinitionInjectorSection::class.java).firstOrNull()?.let {
            if (it.thisDynamicClass == this) return true
        }

        return false
    }
}
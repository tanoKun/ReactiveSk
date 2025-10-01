package com.github.tanokun.reactivesk.skriptadapter.v2_6_3

import ch.njol.skript.SkriptAddon
import ch.njol.skript.lang.TriggerItem
import ch.njol.skript.lang.TriggerSection
import ch.njol.skript.registrations.Classes
import com.github.tanokun.reactivesk.compiler.backend.ClassResolver
import com.github.tanokun.reactivesk.compiler.backend.codegen.JvmBytecodeGenerator
import com.github.tanokun.reactivesk.compiler.backend.intrinsics.VariableFramesIntrinsics
import com.github.tanokun.reactivesk.lang.Identifier
import com.github.tanokun.reactivesk.skriptadapter.common.SkriptAdapter
import com.github.tanokun.reactivesk.skriptadapter.common.reflection.Reflection
import org.koin.core.annotation.Single

@Single(binds = [SkriptAdapter::class])
object SkriptAdapterV263: SkriptAdapter {
    override fun getSkriptClass(className: Identifier): Class<*>? {
        return Classes.getClassInfoNoError(className.identifier.lowercase())?.c
    }

    override fun registerClassToSkript(addon: SkriptAddon) {
    }

    override fun TriggerSection.getFirstInSection(): TriggerItem? {
        val field = Reflection.findField(this.javaClass, "first")

        return field.get(this) as? TriggerItem
    }

    override fun createJvmBytecodeGenerator(
        superClass: Class<*>,
        classResolver: ClassResolver,
        variableFrames: VariableFramesIntrinsics,
        isImplementingBeginFrame: Boolean
    ): JvmBytecodeGenerator {
        TODO("Bytecode を生成する。")
    }
}
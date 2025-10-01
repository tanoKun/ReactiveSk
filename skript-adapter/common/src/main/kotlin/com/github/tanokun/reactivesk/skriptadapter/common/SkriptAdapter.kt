package com.github.tanokun.reactivesk.skriptadapter.common

import ch.njol.skript.SkriptAddon
import ch.njol.skript.lang.TriggerItem
import ch.njol.skript.lang.TriggerSection
import com.github.tanokun.reactivesk.compiler.backend.ClassResolver
import com.github.tanokun.reactivesk.compiler.backend.codegen.JvmBytecodeGenerator
import com.github.tanokun.reactivesk.compiler.backend.intrinsics.VariableFramesIntrinsics
import com.github.tanokun.reactivesk.lang.Identifier

interface SkriptAdapter {
    fun getSkriptClass(className: Identifier): Class<*>?

    fun registerClassToSkript(addon: SkriptAddon)

    fun TriggerSection.getFirstInSection(): TriggerItem?

    fun createJvmBytecodeGenerator(
        superClass: Class<*>,
        classResolver: ClassResolver,
        variableFrames: VariableFramesIntrinsics,
        isImplementingBeginFrame: Boolean
    ): JvmBytecodeGenerator
}
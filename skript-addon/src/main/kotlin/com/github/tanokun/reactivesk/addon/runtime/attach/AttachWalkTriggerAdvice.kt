package com.github.tanokun.reactivesk.addon.runtime.attach

import com.github.tanokun.reactivesk.addon.runtime.AmbiguousVariableFrames
import net.bytebuddy.asm.Advice
import org.bukkit.event.Event

object AttachWalkTriggerAdvice {
    @JvmStatic
    @Advice.OnMethodEnter
    fun onEnter(@Advice.Argument(1) event: Event) {
        AmbiguousVariableFrames.beginFrame(event, 10)
    }

    @JvmStatic
    @Advice.OnMethodExit(onThrowable = Throwable::class)
    fun onExit(@Advice.Argument(1) event: Event) {
        AmbiguousVariableFrames.endFrame(event)
    }
}
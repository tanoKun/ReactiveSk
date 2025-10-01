package com.github.tanokun.reactivesk.addon.runtime.attach

import net.bytebuddy.agent.ByteBuddyAgent
import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.asm.Advice
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.matcher.ElementMatchers
import java.util.logging.Level
import java.util.logging.Logger

object AttachWalkTriggerTransformer {
    fun install(): Boolean {
        try {
            val inst = ByteBuddyAgent.install()

            AgentBuilder.Default()
                .type(ElementMatchers.named("ch.njol.skript.lang.TriggerItem"))
                .transform { builder, _, _, _, _ ->
                    builder
                        .method(ElementMatchers.named<MethodDescription>("walk").and(ElementMatchers.isStatic()))
                        .intercept(Advice.to(AttachWalkTriggerAdvice::class.java))
                }
                .installOn(inst)

            return true

        } catch (e: Exception) {
            Logger.getGlobal().log(Level.SEVERE, "Failed to install AttachWalkTriggerTransformer", e)
        }

        return false
    }
}
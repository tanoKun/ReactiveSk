package com.github.tanokun.reactivesk.v263.intrinsics

import ch.njol.skript.lang.TriggerItem
import com.github.tanokun.reactivesk.compiler.backend.intrinsics.TriggerItemIntrinsics
import org.bukkit.event.Event

object TriggerItemIntrinsicsV263: TriggerItemIntrinsics {
    override fun walk(trigger: Any?, event: Any) {
        if (trigger == null) return

        if (trigger !is TriggerItem) error("trigger is not TriggerItem")
        if (event !is Event) error("event is not a Event")

        TriggerItem.walk(trigger, event)
    }
}
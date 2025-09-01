package com.github.tanokun.addon.runtime.notfiy

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.tanokun.addon.plugin
import com.github.tanokun.addon.runtime.skript.observe.mediator.RuntimeObservingMediator

object ChangeNotifier {
    @JvmStatic
    fun notify(obj: Any, oldValue: Any?, newValue: Any?, reason: String) {
        plugin.launch {
            RuntimeObservingMediator.execute(obj, oldValue, newValue, reason)
        }
    }
}
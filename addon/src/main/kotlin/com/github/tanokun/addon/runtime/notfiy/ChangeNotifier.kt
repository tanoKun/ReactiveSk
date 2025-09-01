package com.github.tanokun.addon.runtime.notfiy

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.tanokun.addon.plugin

object ChangeNotifier {
    @JvmStatic
    fun notify(obj: Any, oldValue: Any?, newValue: Any?, reason: String) {
        plugin.launch {
            println("$obj, $oldValue, $newValue, $reason")
        }
    }
}
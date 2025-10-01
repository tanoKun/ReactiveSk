package com.github.tanokun.reactivesk.addon

import ch.njol.skript.Skript
import ch.njol.skript.SkriptAddon
import com.github.tanokun.reactivesk.addon.runtime.attach.AttachWalkTriggerTransformer
import com.github.tanokun.reactivesk.skriptadapter.common.SkriptAdapter
import org.bukkit.plugin.java.JavaPlugin

lateinit var plugin: ReactiveSkAddon private set

lateinit var skriptAdapter: SkriptAdapter private set

class ReactiveSkAddon : JavaPlugin() {
    lateinit var addon: SkriptAddon
        private set

    private val isAttached: Boolean = AttachWalkTriggerTransformer.install()

    @Suppress("UNCHECKED_CAST")
    override fun onEnable() {
        plugin = this

        addon = Skript.registerAddon(this)

        val skriptVersion = Skript.getVersion()
        val skriptVersionSimple = "${skriptVersion.major}.${skriptVersion.minor}.${skriptVersion.revision}"
        val skriptVersionPackage = skriptVersionSimple.replace(".", "_")
        val skriptVersionClassName = "V${skriptVersionSimple.replace(".", "")}"
        val adapterName = "com.github.tanokun.reactivesk.skriptadapter.v$skriptVersionPackage.SkriptAdapter$skriptVersionClassName"

        try {
            skriptAdapter = Class.forName(adapterName).getDeclaredField("INSTANCE").get(null) as SkriptAdapter
        } catch (_: Exception) {
            logger.severe("This version of Skript ($skriptVersionSimple) is not supported.")
            this.isEnabled = false

            return
        }

        skriptAdapter.registerClassToSkript(addon)

        logger.info("Applying Skript Adapter for version $skriptVersionSimple.")
        logger.info("ReactiveSk Addon has been enabled successfully!")
    }

    override fun onDisable() {
        logger.info("ReactiveSk Addon has been disabled.")
    }
}
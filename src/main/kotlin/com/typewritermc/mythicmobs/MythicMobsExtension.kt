package com.typewritermc.mythicmobs

import com.typewritermc.core.extension.annotations.Singleton
import com.typewritermc.mythicmobs.services.MythicMobVisibilityService
import org.bukkit.Bukkit
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Logger

@Singleton
object MythicMobsExtension : Listener {

    private val plugin by lazy { Bukkit.getPluginManager().getPlugin("TypeWriter")!! }
    val logger: Logger = Logger.getLogger("MythicMobsExtension")

    fun init() {
         // Trigger singleton init
         logger.info("MythicMobsExtension listener registered.")
    }

    init {
        // Register listeners when the singleton is accessed/initialized
        Bukkit.getPluginManager().registerEvents(this, plugin)
        Bukkit.getPluginManager().registerEvents(MythicMobVisibilityService, plugin)
        
        // Start Spawner Service
        com.typewritermc.mythicmobs.services.MythicSpawnerService.start(plugin)
    }
}

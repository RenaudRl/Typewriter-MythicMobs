package com.typewritermc.mythicmobs.services

import com.typewritermc.core.entries.Query
import com.typewritermc.engine.paper.extensions.placeholderapi.parsePlaceholders
import com.typewritermc.engine.paper.utils.toBukkitLocation
import com.typewritermc.mythicmobs.FoliaScheduler
import com.typewritermc.mythicmobs.entries.MythicSpawnerEntry
import io.lumine.mythic.api.mobs.entities.SpawnReason
import io.lumine.mythic.bukkit.BukkitAdapter
import io.lumine.mythic.bukkit.MythicBukkit
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

object MythicSpawnerService : Runnable {

    private val lastSpawnTime = ConcurrentHashMap<String, Long>()
    private val spawnCounts = ConcurrentHashMap<String, Int>()

    fun start(plugin: Plugin) {
        // Run every tick or so. Adjust period as needed.
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, { _ ->
            run()
        }, 1L, 20L) // Run every second (20 ticks) to check spawners
    }

    override fun run() {
        val entries = Query.find(MythicSpawnerEntry::class)
        
        for (entry in entries.asIterable()) {
            handleSpawner(entry)
        }
    }

    private fun handleSpawner(entry: MythicSpawnerEntry) {
        // Basic logic: Check cooldown, check grouping/conditions (omitted for brevity unless requested), spawn.
        
        val now = System.currentTimeMillis() // or ticks
        
        val lastTime = lastSpawnTime.getOrDefault(entry.id, 0L)
        
        // Let's iterate regions.
        for (region in entry.regions) {
             // We pick a random spot in the region
             if (region.corner1.world != region.corner2.world) continue
             val world = Bukkit.getWorld(region.corner1.world.identifier) ?: continue
             
             // Random point logic
             val minX = minOf(region.corner1.x, region.corner2.x)
             val maxX = maxOf(region.corner1.x, region.corner2.x)
             val minZ = minOf(region.corner1.z, region.corner2.z)
             val maxZ = maxOf(region.corner1.z, region.corner2.z)
             val y = region.corner1.y // Assuming flat plane or handling Y differently?
             
             val x = Random.nextDouble(minX, maxX)
             val z = Random.nextDouble(minZ, maxZ)
             
             val loc = org.bukkit.Location(world, x, y, z)
             
             // Check conditions
             // ...
             
             // Spawn
             // Do it safely on Folia
             FoliaScheduler.runAtLocation(loc) {
                 val mobManager = MythicBukkit.inst().mobManager
                 val mobName = entry.mobType // Could be a Var, but entry definition says String
                 val mob = mobManager.getMythicMob(mobName).orElse(null)
                 if (mob != null) {
                     mob.spawn(BukkitAdapter.adapt(loc), 1.0, SpawnReason.SPAWNER)
                 }
             }
        }
    }
}

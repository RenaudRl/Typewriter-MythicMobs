package com.typewritermc.mythicmobs.services

import com.typewritermc.mythicmobs.FoliaScheduler
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

object MythicMobVisibilityService : Listener {

    private val VISIBLE_TO_KEY = NamespacedKey("typewriter", "visible_to")
    private val plugin by lazy { Bukkit.getPluginManager().getPlugin("TypeWriter")!! }

    /**
     * Tags an entity as visible only to a specific player.
     */
    fun setVisibleOnlyTo(entity: Entity, player: Player) {
        // Tag the entity with the player's UUID
        entity.persistentDataContainer.set(VISIBLE_TO_KEY, PersistentDataType.STRING, player.uniqueId.toString())
        
        // Hide from everyone else (This is usually done by the spawning logic, but we can enforce it here if we iterate)
        // Note: Iterating all players is expensive. It's better to rely on `isVisibleByDefault = false` 
        // and then show it to the explicit player. 
        // The calling code (SpawnMobAction) is responsible for setting `isVisibleByDefault = false` 
        // and calling `player.showEntity(plugin, entity)`.
        
        // However, we set the PDC so we can recover this state later.
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        refreshVisibilityForPlayer(event.player)
    }

    @EventHandler
    fun onPlayerChangedWorld(event: PlayerChangedWorldEvent) {
        refreshVisibilityForPlayer(event.player)
    }

    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        refreshVisibilityForPlayer(event.player)
    }

    /**
     * Scans for entities in the player's world that are tagged for them and ensures they are visible.
     */
    private fun refreshVisibilityForPlayer(player: Player) {
        // We delay this slightly to let chunks load and entities appear
        FoliaScheduler.runAtEntityLater(player, 20L) {
             val world = player.world
            // On Folia, we can't iterate world.entities safely from a global context easily?
            // Wait, we are in runAtEntity(player), so we are on the player's region.
            // But world.entities might touch other regions?
            // Actually, Bukkit methods often throw on Folia if accessing state across regions.
            // Safe approach: rely on chunk load events?
            
            // For now, let's try the standard approach for Paper which most users use.
            // On Folia, this might need more robust handling if `world.entities` is not safe.
            // However, typically `world.entities` returns a snapshot or iterates safely? No.
            
            // Optimization: Only scan nearby entities?
            
            if (FoliaScheduler.isFolia()) {
                 // Folia-safe approach: We really should use ChunkLoad events or listen to packet spawns (like BTCMobsNPC).
                 // However, for a simple implementation:
                 // We can use `player.location.getNearbyEntities(...)` which IS safe on Folia (if range is reasonable).
                 val nearby = player.location.getNearbyEntities(100.0, 100.0, 100.0) // 100 block radius
                 nearby.forEach { entity ->
                     checkAndShowEntity(player, entity)
                 }
            } else {
                 // Paper/Spigot safe
                 world.entities.forEach { entity ->
                     checkAndShowEntity(player, entity)
                 }
            }
        }
    }

    private fun checkAndShowEntity(player: Player, entity: Entity) {
        val targetUuidStr = entity.persistentDataContainer.get(VISIBLE_TO_KEY, PersistentDataType.STRING) ?: return
        
        try {
            val targetUuid = UUID.fromString(targetUuidStr)
            if (player.uniqueId == targetUuid) {
                 // It's for this player!
                 // Ensure it is visible.
                 // If it was spawned with `isVisibleByDefault = false`, we need to show it explicitly.
                 player.showEntity(plugin, entity)
            } else {
                // It's for someone else.
                // Ensure it is HIDDEN.
                player.hideEntity(plugin, entity)
            }
        } catch (e: IllegalArgumentException) {
            // Invalid UUID in PDC, ignore
        }
    }
}

package com.typewritermc.mythicmobs.entries.action

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Placeholder
import com.typewritermc.core.extension.annotations.WithRotation
import com.typewritermc.core.utils.point.Position
import com.typewritermc.engine.paper.entry.Criteria
import com.typewritermc.engine.paper.entry.Modifier
import com.typewritermc.engine.paper.entry.TriggerableEntry
import com.typewritermc.engine.paper.entry.entries.ActionEntry
import com.typewritermc.engine.paper.entry.entries.ActionTrigger
import com.typewritermc.engine.paper.entry.entries.ConstVar
import com.typewritermc.engine.paper.entry.entries.Var
import com.typewritermc.engine.paper.extensions.placeholderapi.parsePlaceholders
import com.typewritermc.engine.paper.plugin
import com.typewritermc.engine.paper.utils.toBukkitLocation
import com.typewritermc.mythicmobs.FoliaScheduler
import com.typewritermc.mythicmobs.services.MythicMobVisibilityService
import io.lumine.mythic.api.mobs.entities.SpawnReason
import io.lumine.mythic.bukkit.BukkitAdapter
import io.lumine.mythic.bukkit.MythicBukkit
import org.bukkit.Bukkit

@Entry("spawn_mythicmobs_mob", "Spawn a mob from MythicMobs", Colors.ORANGE, "fa6-solid:dragon")
/**
 * The `Spawn Mob Action` action spawn MythicMobs mobs to the world.
 *
 * ## How could this be used?
 *
 * This action could be used in a plethora of scenarios. From simple quests requiring you to kill some spawned mobs, to complex storylines that simulate entire battles, this action knows no bounds!
 */
class SpawnMobActionEntry(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Placeholder
    val mobName: Var<String> = ConstVar(""),
    val level: Var<Double> = ConstVar(1.0),
    val onlyVisibleForPlayer: Boolean = false,
    @WithRotation
    val spawnLocation: Var<Position> = ConstVar(Position.ORIGIN),
) : ActionEntry {
    override fun ActionTrigger.execute() {
        val mobManager = MythicBukkit.inst().mobManager
        val parsedMobName = mobName.get(player, context).parsePlaceholders(player)
        val mob = mobManager.getMythicMob(parsedMobName)
        
        if (!mob.isPresent) return

        val location = spawnLocation.get(player, context).toBukkitLocation()
        
        // Use FoliaScheduler to spawn safely on the correct thread/region
        FoliaScheduler.runAtLocation(location) {
            try {
                mob.get().spawn(
                    BukkitAdapter.adapt(location),
                    level.get(player, context),
                    SpawnReason.OTHER
                ) { entity ->
                    if (onlyVisibleForPlayer) {
                        entity.isVisibleByDefault = false
                        player.showEntity(plugin, entity)
                        MythicMobVisibilityService.setVisibleOnlyTo(entity, player)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

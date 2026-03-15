package com.typewritermc.mythicmobs.services

import com.typewritermc.mythicmobs.FoliaScheduler
import com.typewritermc.mythicmobs.MythicMobsExtension
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import io.lumine.mythic.bukkit.events.MythicMobSpawnEvent
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent
import io.lumine.mythic.api.mobs.MythicMob
import io.lumine.mythic.bukkit.MythicBukkit
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.persistence.PersistentDataType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages selective visibility of MythicMob entities.
 *
 * Entities can be tagged as visible only to a specific set of players (a "visibility group").
 * This service handles:
 * - Showing/hiding entities based on group membership
 * - Blocking entity targeting of players not in the group
 * - Blocking damage between entities and non-group players (both directions)
 * - Blocking player interactions with non-visible entities
 *
 * Sound and particle isolation is handled separately by [MythicMobPacketFilter] using PacketEvents.
 */
object MythicMobVisibilityService : Listener {

    private val VISIBLE_TO_KEY = NamespacedKey("typewriter", "visible_to_group")
    private val plugin by lazy { Bukkit.getPluginManager().getPlugin("TypeWriter")!! }

    /**
     * In-memory cache for fast lookups. Maps entity UUID → set of allowed player UUIDs.
     * PDC is used for persistence/recovery; this cache avoids PDC reads on hot paths.
     */
    private val visibilityCache = ConcurrentHashMap<UUID, MutableSet<UUID>>()

    /**
     * Thread-safe cache of restricted entity locations to avoid main-thread checks
     * when processing packets in Netty threads.
     */
    private val restrictedEntityLocations = ConcurrentHashMap<UUID, org.bukkit.Location>()

    /**
     * Maps Entity ID -> UUID for restricted entities.
     */
    private val restrictedIdsToUuids = ConcurrentHashMap<Int, UUID>()

    init {
        // Periodically update locations of restricted entities
        FoliaScheduler.runSyncTimer(1L, 1L) {
            visibilityCache.keys.forEach { uuid ->
                val entity = Bukkit.getEntity(uuid) ?: return@forEach
                restrictedEntityLocations[uuid] = entity.location
                restrictedIdsToUuids[entity.entityId] = uuid
            }
        }
    }

    // ─── Public API ──────────────────────────────────────────────────────────────

    /**
     * Tags an entity as visible only to the specified players.
     * The entity MUST already have [Entity.isVisibleByDefault] set to `false`
     * and each target player must have called [Player.showEntity] before this.
     */
    fun setVisibleOnlyTo(entity: Entity, players: Collection<Player>) {
        val uuids = players.map { it.uniqueId }.toMutableSet()
        visibilityCache[entity.uniqueId] = uuids
        persistGroup(entity, uuids)

        // Enforce: hide from everyone, show only to group
        entity.isVisibleByDefault = false
        Bukkit.getOnlinePlayers().forEach { online ->
            if (online.uniqueId in uuids) {
                online.showEntity(plugin, entity)
            } else {
                online.hideEntity(plugin, entity)
            }
        }
    }

    /** Convenience: single-player visibility. */
    fun setVisibleOnlyTo(entity: Entity, player: Player) {
        setVisibleOnlyTo(entity, listOf(player))
    }

    /** Check whether [player] is allowed to see [entity]. */
    fun isVisibleTo(entity: Entity, player: Player): Boolean {
        val group = visibilityCache[entity.uniqueId] ?: return true // No restriction
        return player.uniqueId in group
    }

    /** Returns the set of player UUIDs that may see [entity], or null if unrestricted. */
    fun getVisiblePlayers(entity: Entity): Set<UUID>? {
        return visibilityCache[entity.uniqueId]
    }

    /** Returns true if [entity] has any visibility restriction. */
    fun isRestricted(entity: Entity): Boolean {
        return visibilityCache.containsKey(entity.uniqueId)
    }

    /** Adds a player to the visibility group of an entity. */
    fun addVisiblePlayer(entity: Entity, player: Player) {
        val group = visibilityCache.getOrPut(entity.uniqueId) { mutableSetOf() }
        group.add(player.uniqueId)
        persistGroup(entity, group)
        player.showEntity(plugin, entity)
    }

    /** Removes a player from the visibility group. */
    fun removeVisiblePlayer(entity: Entity, player: Player) {
        val group = visibilityCache[entity.uniqueId] ?: return
        group.remove(player.uniqueId)
        persistGroup(entity, group)
        player.hideEntity(plugin, entity)
    }

    /** Clears all visibility restrictions on an entity. */
    fun clearRestriction(entity: Entity) {
        visibilityCache.remove(entity.uniqueId)
        entity.persistentDataContainer.remove(VISIBLE_TO_KEY)
        entity.isVisibleByDefault = true
    }

    /** Called when a tracked entity is removed from the world. */
    fun onEntityRemoved(entityUuid: UUID) {
        visibilityCache.remove(entityUuid)
        restrictedEntityLocations.remove(entityUuid)
        // Clean up ID mapping (less efficient but safer to avoid stale IDs)
        restrictedIdsToUuids.values.removeIf { it == entityUuid }
    }

    // ─── Event Handlers: Visibility Refresh ──────────────────────────────────────

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

    // ─── Event Handlers: MythicMobs Lifecycle ────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onMythicMobSpawn(event: MythicMobSpawnEvent) {
        val entity = event.entity
        val mob = event.mob
        
        // Check if already handled by SpawnMobAction
        if (isRestricted(entity)) return
        
        // Auto-isolation settings from MythicMob config
        // Example in Mob YML:
        //   Options:
        //     VisibilityGroup: nearby
        val groupOption = mob.type.config.getString("Options.VisibilityGroup")
        
        val reason = event.spawnReason
        val reasonName = reason.toString() // Use string to be safe with enum naming
        if (groupOption?.equals("nearby", ignoreCase = true) == true || 
            reasonName == "NATURAL" || 
            reasonName == "RANDOM" || 
            reasonName == "RANDOM_SPAWN") {
            
            val world = entity.world
            val nearestPlayer = world.players.minByOrNull { it.location.distanceSquared(entity.location) }
            
            if (nearestPlayer != null && nearestPlayer.location.distanceSquared(entity.location) < 100 * 100) {
                setVisibleOnlyTo(entity, nearestPlayer)
                MythicMobsExtension.logger.info("[MythicMobs] Auto-isolated spawn ${mob.type.internalName} to ${nearestPlayer.name} (Reason: $reason, Option: $groupOption)")
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onMythicMobDeath(event: MythicMobDeathEvent) {
        onEntityRemoved(event.entity.uniqueId)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onEntityDeath(event: EntityDeathEvent) {
        onEntityRemoved(event.entity.uniqueId)
    }

    // ─── Event Handlers: Targeting ───────────────────────────────────────────────

    /**
     * Prevent restricted mobs from targeting players who are NOT in their visibility group.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEntityTarget(event: EntityTargetLivingEntityEvent) {
        val target = event.target as? Player ?: return
        val entity = event.entity
        if (!isRestricted(entity)) return
        if (!isVisibleTo(entity, target)) {
            event.isCancelled = true
        }
    }

    // ─── Event Handlers: Damage ──────────────────────────────────────────────────

    /**
     * Block damage in BOTH directions between restricted entities and non-group players:
     * - Mob → Player: mob cannot hurt players who don't see it
     * - Player → Mob: player cannot hurt mobs they can't see
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        val victim = event.entity

        // Case 1: Restricted entity attacks a player
        if (victim is Player && isRestricted(damager)) {
            if (!isVisibleTo(damager, victim)) {
                event.isCancelled = true
                return
            }
        }

        // Case 2: Player attacks a restricted entity
        if (damager is Player && isRestricted(victim)) {
            if (!isVisibleTo(victim, damager)) {
                event.isCancelled = true
                return
            }
        }

        // Case 3: Projectile from a player hits a restricted entity (or vice versa)
        if (damager is org.bukkit.entity.Projectile) {
            val shooter = damager.shooter
            if (shooter is Player && isRestricted(victim)) {
                if (!isVisibleTo(victim, shooter)) {
                    event.isCancelled = true
                    return
                }
            }
        }
        if (victim is Player && damager is org.bukkit.entity.Projectile) {
            val shooter = damager.shooter
            if (shooter is Entity && isRestricted(shooter)) {
                if (!isVisibleTo(shooter, victim)) {
                    event.isCancelled = true
                    return
                }
            }
        }
    }

    // ─── Event Handlers: Interaction ─────────────────────────────────────────────

    /**
     * Block right-click interactions with restricted entities from non-group players.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerInteractAtEntity(event: PlayerInteractAtEntityEvent) {
        val entity = event.rightClicked
        if (!isRestricted(entity)) return
        if (!isVisibleTo(entity, event.player)) {
            event.isCancelled = true
        }
    }

    // ─── Internal Helpers ────────────────────────────────────────────────────────

    /**
     * Scans for restricted entities near the player and enforces visibility.
     */
    private fun refreshVisibilityForPlayer(player: Player) {
        FoliaScheduler.runAtEntityLater(player, 20L) {
            val world = player.world

            if (FoliaScheduler.isFolia()) {
                // Folia-safe: use nearby entities within a reasonable radius
                val nearby = player.location.getNearbyEntities(100.0, 100.0, 100.0)
                nearby.forEach { entity -> enforceVisibility(player, entity) }
            } else {
                // Paper/Spigot: iterate all world entities
                world.entities.forEach { entity -> enforceVisibility(player, entity) }
            }
        }
    }

    private fun enforceVisibility(player: Player, entity: Entity) {
        // First check in-memory cache
        val cachedGroup = visibilityCache[entity.uniqueId]
        if (cachedGroup != null) {
            if (player.uniqueId in cachedGroup) {
                player.showEntity(plugin, entity)
            } else {
                player.hideEntity(plugin, entity)
            }
            return
        }

        // Fallback: check PDC (entity might have been loaded from disk)
        val pdcGroup = loadGroupFromPDC(entity)
        if (pdcGroup != null) {
            // Restore cache
            visibilityCache[entity.uniqueId] = pdcGroup.toMutableSet()
            if (player.uniqueId in pdcGroup) {
                player.showEntity(plugin, entity)
            } else {
                player.hideEntity(plugin, entity)
            }
        }
    }

    private fun persistGroup(entity: Entity, uuids: Set<UUID>) {
        val serialized = uuids.joinToString(",") { it.toString() }
        entity.persistentDataContainer.set(VISIBLE_TO_KEY, PersistentDataType.STRING, serialized)
    }

    fun loadGroupFromPDC(entity: Entity): Set<UUID>? {
        val raw = entity.persistentDataContainer.get(VISIBLE_TO_KEY, PersistentDataType.STRING) ?: return null
        if (raw.isBlank()) return null
        return try {
            raw.split(",").map { UUID.fromString(it.trim()) }.toSet()
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /**
     * Finds restricted entities near the given location thread-safely.
     * Used by PacketFilter from Netty threads.
     */
    fun getNearbyRestricted(loc: org.bukkit.Location, radius: Double): List<UUID> {
        val radiusSq = radius * radius
        return restrictedEntityLocations.entries.filter { (uuid, entityLoc) ->
            entityLoc.world == loc.world && entityLoc.distanceSquared(loc) <= radiusSq
        }.map { it.key }
    }

    /**
     * Checks if an entity with the given ID is restricted.
     * Used by PacketFilter from Netty threads.
     */
    fun isRestricted(entityId: Int): Boolean {
        return restrictedIdsToUuids.containsKey(entityId)
    }

    /**
     * Checks if an entity (by UUID) is visible to a player thread-safely.
     * This uses the thread-safe visibilityCache.
     */
    fun isVisibleTo(entityUuid: UUID, playerUuid: UUID): Boolean {
        val group = visibilityCache[entityUuid] ?: return true
        return playerUuid in group
    }

    /**
     * Checks if an entity (by ID) is visible to a player thread-safely.
     */
    fun isVisibleTo(entityId: Int, playerUuid: UUID): Boolean {
        val uuid = restrictedIdsToUuids[entityId] ?: return true
        return isVisibleTo(uuid, playerUuid)
    }
}

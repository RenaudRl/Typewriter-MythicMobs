package com.typewritermc.mythicmobs.services

import com.typewritermc.core.entries.Query
import com.typewritermc.core.extension.Initializable
import com.typewritermc.core.extension.annotations.Singleton
import com.typewritermc.core.utils.point.distanceSquared
import com.typewritermc.core.utils.point.toPosition as coordinateToPosition
import com.typewritermc.core.utils.point.World as PointWorld
import com.typewritermc.engine.paper.entry.matches
import com.typewritermc.engine.paper.entry.entries.get
import com.typewritermc.engine.paper.logger
import com.typewritermc.engine.paper.plugin
import com.typewritermc.engine.paper.utils.server
import com.typewritermc.engine.paper.utils.toPosition
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.lang.reflect.Method
import com.typewritermc.mythicmobs.entries.MythicSpawnerEntry
import io.lumine.mythic.api.mobs.entities.SpawnReason
import io.lumine.mythic.bukkit.BukkitAdapter
import io.lumine.mythic.bukkit.MythicBukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

@Singleton
object MythicSpawnerService : Initializable, KoinComponent {

    private val lastSpawnTimes = ConcurrentHashMap<String, Long>()
    private val warmupStartTimes = ConcurrentHashMap<String, Long>()
    private val lastActiveTimes = ConcurrentHashMap<String, Long>()

    private var spawnerTask: Any? = null
    private var isRunning = false

    private val isFolia: Boolean by lazy {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    private var getSchedulerMethod: Method? = null
    private var executeMethod: Method? = null

    override suspend fun initialize() {
        isRunning = true

        if (isFolia) {
            try {
                getSchedulerMethod = org.bukkit.entity.Entity::class.java.getMethod("getScheduler")
                val entitySchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler")
                executeMethod = entitySchedulerClass.getMethod("execute", Plugin::class.java, Runnable::class.java, Runnable::class.java, Long::class.javaPrimitiveType)
            } catch (e: Exception) {
                logger.warning("[MythicSpawner] Failed to resolve Folia entity scheduler methods: ${e.message}")
            }
        }

        spawnerTask = if (isFolia) {
            try {
                val globalRegionScheduler = Bukkit::class.java.getMethod("getGlobalRegionScheduler").invoke(null)
                val runMethod = globalRegionScheduler.javaClass.getMethod("runAtFixedRate", Plugin::class.java, java.util.function.Consumer::class.java, Long::class.javaPrimitiveType, Long::class.javaPrimitiveType)
                runMethod.invoke(globalRegionScheduler, plugin, java.util.function.Consumer<Any> { 
                    if (!isRunning) return@Consumer
                    tick() 
                }, 100L, 20L)
            } catch (e: Exception) {
                logger.warning("[MythicSpawner] Failed to schedule Folia global timer: ${e.message}")
                null
            }
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, Runnable { 
                if (!isRunning) return@Runnable
                tick() 
            }, 100L, 20L)
        }
    }

    override suspend fun shutdown() {
        isRunning = false
        try {
            spawnerTask?.let {
                if (isFolia) {
                    it.javaClass.getMethod("cancel").invoke(it)
                } else if (it is org.bukkit.scheduler.BukkitTask) {
                    it.cancel()
                }
            }
        } catch (_: Exception) {}
        spawnerTask = null
        lastSpawnTimes.clear()
        warmupStartTimes.clear()
        lastActiveTimes.clear()
    }

    private fun tick() {
        val spawners = Query.find<MythicSpawnerEntry>().toList()
        if (spawners.isEmpty()) return

        for (player in server.onlinePlayers) {
            if (player.gameMode == GameMode.SPECTATOR) continue
            
            val task = Runnable { processPlayer(player, spawners) }
            
            if (isFolia && getSchedulerMethod != null && executeMethod != null) {
                try {
                    val scheduler = getSchedulerMethod!!.invoke(player)
                    executeMethod!!.invoke(scheduler, plugin, task, null, 0L)
                } catch (e: Exception) {
                    // Fallback or ignore if entity is gone
                }
            } else {
                Bukkit.getScheduler().runTask(plugin, task)
            }
        }
    }

    private fun processPlayer(player: Player, spawners: List<MythicSpawnerEntry>) {
        val currentTime = System.currentTimeMillis()

        for (entry in spawners) {
            val uniqueKey = "${player.world.name}_${entry.id}"
            logger.finer("[MythicSpawner] Checking spawner ${entry.id} for player ${player.name}")

            val activeRegion = entry.regions.firstOrNull { region ->
                val range = region.activationRangeFact.get()?.readForPlayersGroup(player)?.value?.toDouble() ?: region.defaultActivationRange
                val pos = region.center().coordinateToPosition(PointWorld(player.world.uid.toString()))
                val dist = pos.distanceSquared(player.location.x, player.location.y, player.location.z)
                
                logger.finer("[MythicSpawner] Checking region ${region.center()} - dist: $dist (requires <= ${range * range})")
                dist <= range * range
            }

            if (activeRegion == null) {
                logger.finer("[MythicSpawner] Player ${player.name} not in any active region for ${entry.id}")
                continue
            }

            // Check Criteria
            if (entry.criteria.isNotEmpty()) {
                val centerLoc = org.bukkit.Location(player.world, activeRegion.center().x, activeRegion.center().y, activeRegion.center().z)
                val criteriaRange = (activeRegion.activationRangeFact.get()?.readForPlayersGroup(player)?.value?.toDouble() ?: activeRegion.defaultActivationRange) + 5.0
                val eligiblePlayers = player.world.getNearbyEntities(centerLoc, criteriaRange, criteriaRange, criteriaRange)
                    .filterIsInstance<Player>()
                
                if (eligiblePlayers.none { entry.criteria.matches(it) }) {
                    logger.finer("[MythicSpawner] Criteria check failed for all eligible nearby players for spawner ${entry.id}")
                    continue
                }
            }

            // Read properties
            val maxMobs = entry.maxMobsFact.get()?.readForPlayersGroup(player)?.value ?: entry.defaultMaxMobs
            val mobsPerSpawn = entry.mobsPerSpawnFact.get()?.readForPlayersGroup(player)?.value ?: entry.defaultMobsPerSpawn
            val cooldownTicks = entry.cooldownFact.get()?.readForPlayersGroup(player)?.value?.toLong() ?: entry.defaultCooldown
            val warmupTicks = entry.warmupFact.get()?.readForPlayersGroup(player)?.value?.toLong() ?: entry.defaultWarmup
            
            lastActiveTimes[uniqueKey] = currentTime

            // Warmup
            if (warmupTicks > 0) {
                val start = warmupStartTimes.computeIfAbsent(uniqueKey) { currentTime }
                if (currentTime - start < warmupTicks * 50) {
                    logger.finer("[MythicSpawner] Spawner ${entry.id} in warmup for another ${(warmupTicks * 50) - (currentTime - start)}ms")
                    continue
                }
            }

            // Cooldown
            val timeSinceLast = currentTime - lastSpawnTimes.getOrDefault(uniqueKey, 0L)
            if (timeSinceLast < cooldownTicks * 50) {
                logger.finer("[MythicSpawner] Spawner ${entry.id} on cooldown for another ${(cooldownTicks * 50) - timeSinceLast}ms")
                continue
            }

            // Count nearby mobs
            val centerLoc = org.bukkit.Location(player.world, activeRegion.center().x, activeRegion.center().y, activeRegion.center().z)
            val mobRange = (activeRegion.activationRangeFact.get()?.readForPlayersGroup(player)?.value?.toDouble() ?: activeRegion.defaultActivationRange) + 5.0
            
            val nearby = player.world.getNearbyEntities(
                centerLoc, 
                mobRange, 
                mobRange, 
                mobRange
            )

            val api = MythicBukkit.inst().mobManager
            val mmInfo = api.getMythicMob(entry.mobType).orElse(null) 
            if (mmInfo == null) {
                logger.warning("[MythicSpawner] Cannot find MythicMob ${entry.mobType} for spawner ${entry.id}")
                continue
            }

            val count = nearby.count { entity ->
                if (api.isActiveMob(entity.uniqueId)) {
                    val activeMob = api.getMythicMobInstance(entity)
                    activeMob?.mobType == entry.mobType
                } else false
            }

            logger.finer("[MythicSpawner] Active mobs count for ${entry.id} is $count / $maxMobs")

            if (count >= maxMobs) {
                lastSpawnTimes[uniqueKey] = currentTime
                logger.finer("[MythicSpawner] Max mobs reached for spawner ${entry.id}")
                continue
            }

            val spawnCount = min(mobsPerSpawn, maxMobs - count)
            if (spawnCount <= 0) {
                lastSpawnTimes[uniqueKey] = currentTime
                continue
            }

            // Resolve visibility targets for this spawner
            val visiblePlayers: List<Player>? = resolveVisibility(entry, player)

            logger.info("[MythicSpawner] Spawning $spawnCount x ${entry.mobType} from entry ${entry.id}")
            repeat(spawnCount) {
                val pt = activeRegion.randomSpawnPoint()
                val spawnLoc = org.bukkit.Location(player.world, pt.x, pt.y, pt.z)
                mmInfo.spawn(BukkitAdapter.adapt(spawnLoc), 1.0, SpawnReason.SPAWNER) { entity ->
                    if (visiblePlayers != null) {
                        entity.isVisibleByDefault = false
                        visiblePlayers.forEach { p -> p.showEntity(plugin, entity) }
                        MythicMobVisibilityService.setVisibleOnlyTo(entity, visiblePlayers)
                    }
                }
            }
            
            lastSpawnTimes[uniqueKey] = currentTime
        }
    }

    /**
     * Resolves the visibility targets for a spawner entry.
     * @return list of visible players if restricted, null if unrestricted
     */
    private fun resolveVisibility(entry: MythicSpawnerEntry, triggerPlayer: Player): List<Player>? {
        // Group-based visibility takes priority
        val groupEntry = entry.visibilityGroup.get()
        if (groupEntry != null) {
            val group = groupEntry.group(triggerPlayer)
            if (group != null && group.players.isNotEmpty()) {
                return group.players
            }
            return listOf(triggerPlayer)
        }

        // Single-player visibility
        if (entry.onlyVisibleForNearby) {
            return listOf(triggerPlayer)
        }

        // No restriction
        return null
    }
}


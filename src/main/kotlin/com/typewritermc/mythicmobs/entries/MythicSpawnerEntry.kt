package com.typewritermc.mythicmobs.entries

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.utils.point.Coordinate
import com.typewritermc.engine.paper.entry.StaticEntry
import com.typewritermc.engine.paper.entry.entries.GroupEntry
import com.typewritermc.engine.paper.entry.entries.ReadableFactEntry
import com.typewritermc.engine.paper.entry.Criteria
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random


data class SpawnerRegion(
    @Help("First corner of the spawn zone (absolute world coordinates)")
    val corner1: Coordinate = Coordinate.ORIGIN,
    @Help("Second corner of the spawn zone (absolute world coordinates)")
    val corner2: Coordinate = Coordinate.ORIGIN,
    @Help("Optional fact for global activation range from region center. If not set, uses default.")
    val activationRangeFact: Ref<ReadableFactEntry> = emptyRef(),
    @Help("Default global activation range (3D radius) for player detection")
    val defaultActivationRange: Double = 24.0,
) : java.io.Serializable {
    fun center(): Coordinate = Coordinate(
        (corner1.x + corner2.x) / 2.0,
        (corner1.y + corner2.y) / 2.0,
        (corner1.z + corner2.z) / 2.0
    )

    fun minX() = min(corner1.x, corner2.x)
    fun maxX() = max(corner1.x, corner2.x)
    fun minY() = min(corner1.y, corner2.y)
    fun maxY() = max(corner1.y, corner2.y)
    fun minZ() = min(corner1.z, corner2.z)
    fun maxZ() = max(corner1.z, corner2.z)

    fun randomSpawnPoint(): Coordinate {
        val x = minX() + Random.nextDouble() * (maxX() - minX())
        val y = minY() + Random.nextDouble() * (maxY() - minY())
        val z = minZ() + Random.nextDouble() * (maxZ() - minZ())
        return Coordinate(x, y, z)
    }
}

@Entry("mythic_spawner", "MythicMobs Spawner with Corner System", Colors.RED, "fa6-solid:dungeon")
class MythicSpawnerEntry(
    override val id: String = "",
    override val name: String = "",
    
    @Help("The internal name of the MythicMob to spawn")
    val mobType: String = "SkeletonKing",
    
    @Help("Spawn regions - each defines a zone where mobs can spawn using corners")
    val regions: List<SpawnerRegion> = listOf(SpawnerRegion()),
    
    @Help("Max mobs allowed in the area. Fact value used directly. Default: 1")
    val maxMobsFact: Ref<ReadableFactEntry> = emptyRef(),
    @Help("Fallback max mobs if fact is not set")
    val defaultMaxMobs: Int = 1,
    
    @Help("Number of mobs to spawn per spawn cycle. Fact value used directly. Default: 1")
    val mobsPerSpawnFact: Ref<ReadableFactEntry> = emptyRef(),
    @Help("Fallback mobs per spawn if fact is not set")
    val defaultMobsPerSpawn: Int = 1,
    
    @Help("Cooldown between spawns in ticks. Fact value used directly. Default: 100")
    val cooldownFact: Ref<ReadableFactEntry> = emptyRef(),
    @Help("Fallback cooldown in ticks if fact is not set")
    val defaultCooldown: Long = 100L,
    
    @Help("Warmup time before first spawn in ticks. Fact value used directly. Default: 0")
    val warmupFact: Ref<ReadableFactEntry> = emptyRef(),
    @Help("Fallback warmup time in ticks if fact is not set")
    val defaultWarmup: Long = 0L,
    
    @Help("The group used to filter players and read facts from. If not set, uses the player directly.")
    val group: Ref<GroupEntry> = emptyRef(),

    @Help("The criteria that must be met by at least one nearby player for the spawner to be active")
    val criteria: List<Criteria> = emptyList(),

    @Help("If true, spawned mobs are visible only to the nearest player who triggered the spawn")
    val onlyVisibleForNearby: Boolean = false,

    @Help("Optional group reference. If set, spawned mobs are visible only to members of this group. Takes priority over onlyVisibleForNearby.")
    val visibilityGroup: Ref<GroupEntry> = emptyRef(),
) : StaticEntry

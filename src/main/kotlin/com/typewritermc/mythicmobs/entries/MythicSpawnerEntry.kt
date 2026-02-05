package com.typewritermc.mythicmobs.entries

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.entry.StaticEntry
import com.typewritermc.engine.paper.entry.entries.ConstVar
import com.typewritermc.engine.paper.entry.entries.GroupEntry
import com.typewritermc.engine.paper.entry.entries.Var

@Entry("mythic_spawner", "MythicMobs Spawner with Corner System", Colors.RED, "fa6-solid:dungeon")
class MythicSpawnerEntry(
    override val id: String = "",
    override val name: String = "",
    
    @Help("The internal name of the MythicMob to spawn")
    val mobType: String = "SkeletonKing",
    
    @Help("Spawn regions - each defines a zone where mobs can spawn using corners")
    val regions: List<SpawnerRegion> = listOf(SpawnerRegion()),
    
    @Help("Max mobs allowed in the area.")
    val maxMobs: Var<Int> = ConstVar(1),
    
    @Help("Number of mobs to spawn per spawn cycle.")
    val mobsPerSpawn: Var<Int> = ConstVar(1),
    
    @Help("Cooldown between spawns in ticks.")
    val cooldown: Var<Long> = ConstVar(100L),
    
    @Help("Warmup time before first spawn in ticks.")
    val warmup: Var<Long> = ConstVar(0L),
    
    @Help("The group used to filter players and read facts from.")
    val group: Ref<GroupEntry> = emptyRef(),
) : StaticEntry

data class SpawnerRegion(
    var corner1: com.typewritermc.core.utils.point.Position = com.typewritermc.core.utils.point.Position.ORIGIN,
    var corner2: com.typewritermc.core.utils.point.Position = com.typewritermc.core.utils.point.Position.ORIGIN,
) : java.io.Serializable

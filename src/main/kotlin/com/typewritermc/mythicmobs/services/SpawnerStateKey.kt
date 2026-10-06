package com.typewritermc.mythicmobs.services

/**
 * Key of the warmup and cooldown state of a spawner.
 *
 * Without a group, every player shares the state of the spawner in a world (the historical key,
 * unchanged). With a group, each group id has its own state, so two parties standing near the same
 * spawner each get their own spawn cycle instead of one consuming the other's cooldown.
 */
internal fun spawnerStateKey(worldName: String, spawnerId: String, groupId: String?): String =
    if (groupId == null) "${worldName}_$spawnerId" else "${worldName}_${spawnerId}_group_$groupId"

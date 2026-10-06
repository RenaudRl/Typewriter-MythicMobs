package com.typewritermc.mythicmobs.services

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Remembers which players kept alive each per-group spawner state key.
 *
 * A group key holds warmup and cooldown state that is only meaningful while a member of that group is
 * online. [release] reports the keys that lost their last online member so the caller can drop their
 * state; keys without a group are never registered here, so they are never reported.
 */
internal class GroupStateRegistry {

    private val membersByKey = ConcurrentHashMap<String, MutableSet<UUID>>()

    /** Notes that [playerId] was seen in the group that owns [key]. */
    fun record(key: String, playerId: UUID) {
        membersByKey.computeIfAbsent(key) { ConcurrentHashMap.newKeySet() }.add(playerId)
    }

    /** Forgets members not in [onlinePlayers] and returns the keys left without any member. */
    fun release(onlinePlayers: Set<UUID>): List<String> {
        val released = mutableListOf<String>()
        for (key in membersByKey.keys.toList()) {
            val remaining = membersByKey.computeIfPresent(key) { _, members ->
                members.retainAll(onlinePlayers)
                if (members.isEmpty()) null else members
            }
            if (remaining == null) released += key
        }
        return released
    }

    fun clear() = membersByKey.clear()
}

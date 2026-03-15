package com.typewritermc.mythicmobs.utils

import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.priority
import com.typewritermc.engine.paper.entry.entries.AudienceFilter
import com.typewritermc.engine.paper.entry.matches
import com.typewritermc.engine.paper.facts.FactListenerSubscription
import com.typewritermc.quest.*
import com.typewritermc.quest.entries.*
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

abstract class ObjectiveDisplay<T : ObjectiveEntry>(
    val ref: Ref<T>,
) : AudienceFilter(ref) {
    private val factWatcherSubscriptions = ConcurrentHashMap<UUID, FactListenerSubscription>()

    override fun filter(player: Player): Boolean {
        return ref.get()?.criteria?.matches(player) == true
    }

    override fun onPlayerFilterAdded(player: Player) {
        super.onPlayerFilterAdded(player)
        val quest = ref.get()?.quest ?: return

        if (!player.isQuestActive(quest)) {
            return
        }

        if (player.trackedQuest() == null) {
            player.trackQuest(quest)
            return
        }

        player.trackedShowingObjectives().maxOfOrNull { it.priority }?.let { highestPriority ->
            if (ref.priority >= highestPriority) {
                player.trackQuest(quest)
            }
        } ?: player.trackQuest(quest)
    }
}

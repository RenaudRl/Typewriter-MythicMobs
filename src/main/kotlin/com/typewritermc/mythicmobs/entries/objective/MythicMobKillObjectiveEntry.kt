package com.typewritermc.mythicmobs.entries.objective

import com.typewritermc.mythicmobs.utils.BaseCountObjectiveDisplay
import com.typewritermc.mythicmobs.utils.BaseCountObjectiveEntry
import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.entries.ref
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.entry.Criteria
import com.typewritermc.engine.paper.entry.Modifier
import com.typewritermc.engine.paper.entry.TriggerableEntry
import com.typewritermc.engine.paper.entry.entries.*
import com.typewritermc.quest.entries.QuestEntry
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import java.util.*
import kotlin.text.RegexOption
import com.typewritermc.engine.paper.entry.entries.AudienceFilter

@Entry(
    "mythicmob_kill_objective",
    "An objective to kill MythicMobs mobs",
    Colors.PURPLE,
    "fa6-solid:dragon"
)
class MythicMobKillObjectiveEntry(
    override val id: String = "",
    override val name: String = "",
    override val quest: Ref<QuestEntry> = emptyRef(),
    override val criteria: List<Criteria> = emptyList(),
    override val children: List<Ref<AudienceEntry>> = emptyList(),
    override val fact: Ref<CachableFactEntry> = emptyRef(),
    @Help("The MythicMob internal name regex to match. Leave empty to count any MythicMob.")
    val mythicMobName: Var<String> = ConstVar(""),
    @Help("The amount of MythicMobs to kill.")
    override val amount: Var<Int> = ConstVar(1),
    override val display: Var<String> = ConstVar(""),
    override val onComplete: Ref<TriggerableEntry> = emptyRef(),
    override val onCompleteModifiers: List<Modifier> = emptyList(),
    override val priorityOverride: Optional<Int> = Optional.empty(),
) : BaseCountObjectiveEntry {
    override suspend fun display(): AudienceFilter {
        return MythicMobKillObjectiveDisplay(ref())
    }
}

private class MythicMobKillObjectiveDisplay(ref: Ref<MythicMobKillObjectiveEntry>) :
    BaseCountObjectiveDisplay<MythicMobKillObjectiveEntry>(ref) {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onMythicMobDeath(event: MythicMobDeathEvent) {
        val player = event.killer as? Player ?: return
        val entry = ref.get() ?: return
        if (!filter(player)) return

        val requiredName = entry.mythicMobName.get(player)
        if (requiredName.isBlank()) {
            incrementCount(player)
            return
        }

        val internalName = event.mobType.internalName
        val regex = runCatching { requiredName.toRegex(RegexOption.IGNORE_CASE) }.getOrNull()
            ?: return

        if (regex.matches(internalName)) {
            incrementCount(player)
        }
    }
}

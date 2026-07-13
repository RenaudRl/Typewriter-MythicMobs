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
import com.typewritermc.quest.trackedQuest
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.entity.Tameable
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
    @Help("List of MythicMob internal names to match. If provided, mythicMobName regex is ignored.")
    val mobs: List<Var<String>> = emptyList(),
    @Help("The amount of MythicMobs to kill.")
    override val amount: Var<Int> = ConstVar(1),
    override val display: Var<String> = ConstVar(""),
    override val onComplete: Ref<TriggerableEntry> = emptyRef(),
    override val onCompleteModifiers: List<Modifier> = emptyList(),
    override val priorityOverride: Optional<Int> = Optional.empty(),
    @Help("Whether to hide this objective from the tracking menu (scoreboard/placeholders) unless the quest is tracked.")
    val hideObjective: Var<Boolean> = ConstVar(false)
) : BaseCountObjectiveEntry {

    override fun display(player: Player?): String {
        if (player != null && hideObjective.get(player) && player.trackedQuest() != quest) {
            return ""
        }
        return super.display(player)
    }

    override suspend fun display(): AudienceFilter {
        return MythicMobKillObjectiveDisplay(ref())
    }
}

private class MythicMobKillObjectiveDisplay(ref: Ref<MythicMobKillObjectiveEntry>) :
    BaseCountObjectiveDisplay<MythicMobKillObjectiveEntry>(ref) {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onMythicMobDeath(event: MythicMobDeathEvent) {
        val player = resolvePlayer(event) ?: return
        val entry = ref.get() ?: return
        if (!filter(player)) return

        val internalName = event.mobType.internalName

        // Priority 1: mobs list
        if (entry.mobs.isNotEmpty()) {
            val matched = entry.mobs.any { it.get(player).equals(internalName, ignoreCase = true) }
            if (matched) {
                incrementCount(player)
            }
            return
        }

        // Priority 2: regex mythicMobName
        val requiredName = entry.mythicMobName.get(player)
        if (requiredName.isBlank()) {
            incrementCount(player)
            return
        }

        val regex = runCatching { requiredName.toRegex(RegexOption.IGNORE_CASE) }.getOrNull()
            ?: return

        if (regex.matches(internalName)) {
            incrementCount(player)
        }
    }

    /**
     * Résout le joueur à partir de l'event en remontant la chaîne de cause :
     * 1. killer direct si c'est un Player
     * 2. shooter du projectile (flèche, trident, etc.)
     * 3. owner d'un mob apprivoisé (loup, chat)
     * 4. Fallback Bukkit entity.killer
     */
    private fun resolvePlayer(event: MythicMobDeathEvent): Player? {
        val killer = event.killer

        // Cas 1 : killer direct
        if (killer is Player) return killer

        // Cas 2 : projectile (arc, trident, potion)
        if (killer is Projectile) {
            val shooter = killer.shooter
            if (shooter is Player) return shooter
        }

        // Cas 3 : mob apprivoisé (loup, chat, etc.)
        if (killer is Tameable) {
            val owner = killer.owner
            if (owner is Player) return owner
        }

        // Cas 4 : Fallback Bukkit — gère déjà projectiles/indirect
        val livingEntity = event.entity as? LivingEntity
        return livingEntity?.killer
    }
}

package com.typewritermc.mythicmobs.utils

import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.interaction.context
import com.typewritermc.engine.paper.entry.Modifier
import com.typewritermc.engine.paper.entry.TriggerableEntry
import com.typewritermc.engine.paper.entry.entries.CachableFactEntry
import com.typewritermc.engine.paper.entry.entries.Var
import com.typewritermc.engine.paper.entry.matches
import com.typewritermc.engine.paper.entry.triggerFor
import com.typewritermc.engine.paper.extensions.placeholderapi.parsePlaceholders
import com.typewritermc.engine.paper.facts.FactDatabase
import com.typewritermc.engine.paper.snippets.snippet
import com.typewritermc.engine.paper.entry.entries.get
import com.typewritermc.engine.paper.utils.asMini
import com.typewritermc.engine.paper.utils.asMiniWithResolvers
import com.typewritermc.engine.paper.utils.replaceTagPlaceholders
import com.typewritermc.quest.entries.ObjectiveEntry
import com.typewritermc.quest.entries.inactiveObjectiveDisplay
import com.typewritermc.quest.isQuestActive
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed
import org.bukkit.entity.Player
import org.koin.java.KoinJavaComponent.get
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.absoluteValue
import com.typewritermc.engine.paper.entry.Criteria

private val displaySnippet by snippet(
    "advancedQuest.display",
    "<display> <dark_gray>(<gray><current><dark_gray>/<gray><required><dark_gray>)"
)

private val completedDisplaySnippet by snippet(
    "advancedQuest.completed",
    "<green>✔</green> <st><display></st> <dark_gray>(<gray><current><dark_gray>/<gray><required><dark_gray>)"
)

interface BaseCountObjectiveEntry : ObjectiveEntry {
    @Help("The fact that is used to track the player's progress.")
    val fact: Ref<CachableFactEntry>

    @Help("The required amount to complete the objective.")
    val amount: Var<Int>

    @Help("The sequence to trigger when the player completes the objective.")
    val onComplete: Ref<TriggerableEntry>
        get() = emptyRef()

    @Help("The modifiers applied when the player completes the objective.")
    val onCompleteModifiers: List<Modifier>
        get() = emptyList()

    override fun display(player: Player?): String {
        val displayText = display.get(player) ?: ""

        if (player == null || !player.isQuestActive(quest) || !criteria.matches(player)) {
            return inactiveObjectiveDisplay.asMiniWithResolvers(parsed("display", displayText)).asMini().parsePlaceholders(player)
        }

        val factEntry = fact.get() ?: return inactiveObjectiveDisplay.asMiniWithResolvers(parsed("display", displayText)).asMini().parsePlaceholders(player)
        val currentValue = factEntry.readForPlayersGroup(player).value

        val requiredAmount = amount.get(player).absoluteValue
        val isComplete = currentValue >= requiredAmount

        val text = if (isComplete) completedDisplaySnippet else displaySnippet

        return text.asMiniWithResolvers(
            parsed("display", displayText),
            parsed("current", currentValue.toString()),
            parsed("required", requiredAmount.toString())
        ).asMini().parsePlaceholders(player)
    }
}

abstract class BaseCountObjectiveDisplay<T : BaseCountObjectiveEntry>(ref: Ref<T>) :
    ObjectiveDisplay<T>(ref) {

    private val lastKnownValues = ConcurrentHashMap<UUID, Int>()
    private val completionStatus = ConcurrentHashMap<UUID, Boolean>()

    private fun applyCompletionModifiers(entry: BaseCountObjectiveEntry, player: Player) {
        val modifiers = entry.onCompleteModifiers
        if (modifiers.isEmpty()) {
            return
        }

        val factDatabase: FactDatabase = get(FactDatabase::class.java)
        factDatabase.modify(player, modifiers, context())
    }

    @Volatile
    private var cachedRequiredAmount: Int? = null

    override fun onPlayerAdd(player: Player) {
        val entry = ref.get() ?: return

        if (cachedRequiredAmount == null) {
            cachedRequiredAmount = entry.amount.get(player).absoluteValue
        }

        val fact = entry.fact.get()
        val isCurrentlyComplete =
            if (player.isQuestActive(entry.quest) && fact != null) {
                val currentValue = fact.readForPlayersGroup(player).value
                lastKnownValues[player.uniqueId] = currentValue
                currentValue >= (cachedRequiredAmount ?: 0)
            } else false

        completionStatus[player.uniqueId] = isCurrentlyComplete
        super.onPlayerAdd(player)
    }

    override fun onPlayerRemove(player: Player) {
        super.onPlayerRemove(player)
        val playerId = player.uniqueId
        completionStatus.remove(playerId)
        lastKnownValues.remove(playerId)
    }

    private fun checkForExternalChanges(player: Player): Boolean {
        val entry = ref.get() ?: return false
        if (!player.isQuestActive(entry.quest)) return false
        val fact = entry.fact.get() ?: return false
        val playerId = player.uniqueId

        val currentValue = fact.readForPlayersGroup(player).value
        val lastKnownValue = lastKnownValues[playerId] ?: currentValue

        if (currentValue != lastKnownValue) {
            lastKnownValues[playerId] = currentValue

            val requiredAmount = cachedRequiredAmount ?: entry.amount.get(player).absoluteValue
            val wasComplete = completionStatus[playerId] ?: false
            val isNowComplete = currentValue >= requiredAmount

            if (isNowComplete != wasComplete) {
                completionStatus[playerId] = isNowComplete

                if (!wasComplete) {
                    applyCompletionModifiers(entry, player)
                    entry.onComplete.triggerFor(player, context())
                    return true
                }
            }
        }

        return false
    }

    protected fun incrementCount(player: Player, incrementAmount: Int = 1) {
        val entry = ref.get() ?: return
        if (!player.isQuestActive(entry.quest)) return
        val fact = entry.fact.get() ?: return
        val playerId = player.uniqueId

        checkForExternalChanges(player)

        val wasComplete = completionStatus[playerId] ?: false
        val requiredAmount = cachedRequiredAmount ?: entry.amount.get(player).absoluteValue

        val currentValue = fact.readForPlayersGroup(player).value
        val newValue = currentValue + incrementAmount
        fact.write(player, newValue)

        lastKnownValues[playerId] = newValue

        val isNowComplete = newValue >= requiredAmount

        if (isNowComplete != wasComplete) {
            completionStatus[playerId] = isNowComplete

            if (!wasComplete) {
                applyCompletionModifiers(entry, player)
                entry.onComplete.triggerFor(player, context())
            }
        }
    }
}

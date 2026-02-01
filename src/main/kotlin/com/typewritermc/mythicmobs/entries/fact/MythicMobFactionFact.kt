package com.typewritermc.mythicmobs.entries.fact

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Placeholder
import com.typewritermc.engine.paper.entry.Criteria
import com.typewritermc.engine.paper.entry.Modifier
import com.typewritermc.engine.paper.entry.TriggerableEntry
import com.typewritermc.engine.paper.entry.entries.ConstVar
import com.typewritermc.engine.paper.entry.entries.FactEntry
import com.typewritermc.engine.paper.entry.entries.Var
import com.typewritermc.engine.paper.extensions.placeholderapi.parsePlaceholders
import io.lumine.mythic.bukkit.MythicBukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

import com.typewritermc.engine.paper.entry.entries.GroupEntry
import com.typewritermc.engine.paper.entry.entries.ReadableFactEntry
import com.typewritermc.engine.paper.facts.FactData
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef

@Entry("mythicmob_faction", "MythicMob Faction", Colors.BLUE, "fa6-solid:flag")
class MythicMobFactionFact(
    override val id: String = "",
    override val name: String = "",
    override val comment: String = "",
    override val group: Ref<GroupEntry> = emptyRef(),
    @Placeholder
    val faction: Var<String> = ConstVar(""),
) : ReadableFactEntry {
    override fun readSinglePlayer(player: Player): FactData {
        return FactData(0)
    }
}

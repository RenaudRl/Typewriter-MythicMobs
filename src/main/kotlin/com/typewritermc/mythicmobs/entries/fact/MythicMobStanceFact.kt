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
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.engine.paper.entry.entries.GroupEntry
import com.typewritermc.engine.paper.entry.entries.ReadableFactEntry
import com.typewritermc.engine.paper.facts.FactData

@Entry("mythicmob_stance", "MythicMob Stance", Colors.BLUE, "fa6-solid:ruler")
class MythicMobStanceFact(
    override val id: String = "",
    override val name: String = "",
    override val comment: String = "",
    override val group: Ref<GroupEntry> = emptyRef(),
    @Placeholder
    val stance: Var<String> = ConstVar(""),
) : ReadableFactEntry {
    override fun readSinglePlayer(player: Player): FactData {
        // Context-aware checking is not supported by ReadableFactEntry directly.
        // This fact is currently a placeholder or requires a different approach (e.g. PlaceholderAPI).
        return FactData(0)
    }
}

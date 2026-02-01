package com.typewritermc.mythicmobs.entries.fact

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.engine.paper.entry.entries.GroupEntry
import com.typewritermc.engine.paper.entry.entries.ReadableFactEntry
import com.typewritermc.engine.paper.facts.FactData
import io.lumine.mythic.bukkit.MythicBukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

@Entry("mythicmob_level", "MythicMob Level", Colors.BLUE, "fa6-solid:layer-group")
class MythicMobLevelFact(
    override val id: String = "",
    override val name: String = "",
    override val comment: String = "",
    override val group: Ref<GroupEntry> = emptyRef(),
) : ReadableFactEntry {
    override fun readSinglePlayer(player: Player): FactData {
        return FactData(0)
    }
}

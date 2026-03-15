package com.typewritermc.mythicmobs.services

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntitySoundEffect
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSoundEffect
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerParticle
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.UUID
import java.util.logging.Level

/**
 * PacketEvents-based packet filter for MythicMob visibility isolation.
 *
 * Intercepts outgoing sound and particle packets to ensure they are only
 * received by players in the entity's visibility group.
 */
object MythicMobPacketFilter : PacketListenerAbstract(PacketListenerPriority.HIGH) {

    private val logger = Bukkit.getLogger()

    /**
     * Registers this filter with the PacketEvents API.
     * Should be called during extension initialization.
     */
    fun register() {
        try {
            val api = PacketEvents.getAPI()
            if (api != null && api.isLoaded) {
                api.eventManager.registerListener(this)
                logger.info("[MythicMobs] Packet filter registered with PacketEvents")
            } else {
                logger.warning("[MythicMobs] PacketEvents not loaded — sound isolation disabled")
            }
        } catch (e: Throwable) {
            logger.log(Level.WARNING, "[MythicMobs] Failed to register sound filter: ${e.message}")
        }
    }

    /**
     * Unregisters this filter from PacketEvents.
     * Should be called during extension shutdown.
     */
    fun unregister() {
        try {
            val api = PacketEvents.getAPI()
            if (api != null && api.isLoaded) {
                api.eventManager.unregisterListener(this)
            }
        } catch (_: Throwable) {
            // Silently ignore — plugin might be shutting down
        }
    }

    override fun onPacketSend(event: PacketSendEvent) {
        val type = event.packetType
        if (type != PacketType.Play.Server.ENTITY_SOUND_EFFECT && 
            type != PacketType.Play.Server.SOUND_EFFECT &&
            type != PacketType.Play.Server.NAMED_SOUND_EFFECT &&
            type != PacketType.Play.Server.PARTICLE) return

        try {
            // Resolve the Bukkit player receiving this packet
            val receiverUuid = event.user.uuid ?: return
            
            if (type == PacketType.Play.Server.ENTITY_SOUND_EFFECT) {
                val wrapper = WrapperPlayServerEntitySoundEffect(event)
                val entityId = wrapper.entityId
                
                if (MythicMobVisibilityService.isRestricted(entityId)) {
                    if (!MythicMobVisibilityService.isVisibleTo(entityId, receiverUuid)) {
                        event.isCancelled = true
                    }
                }
            } else if (type == PacketType.Play.Server.SOUND_EFFECT || type == PacketType.Play.Server.NAMED_SOUND_EFFECT) {
                val wrapper = WrapperPlayServerSoundEffect(event)
                val effectPos = wrapper.getEffectPosition()
                val receiver = Bukkit.getPlayer(receiverUuid) ?: return
                val world = receiver.world
                val soundLoc = Location(world, effectPos.getX() / 8.0, effectPos.getY() / 8.0, effectPos.getZ() / 8.0)
                
                val nearbyRestrictedUuids = MythicMobVisibilityService.getNearbyRestricted(soundLoc, 0.5)

                if (nearbyRestrictedUuids.isNotEmpty()) {
                    if (nearbyRestrictedUuids.any { !MythicMobVisibilityService.isVisibleTo(it, receiverUuid) }) {
                        event.isCancelled = true
                        return
                    }
                }
            } else if (type == PacketType.Play.Server.PARTICLE) {
                val wrapper = WrapperPlayServerParticle(event)
                val position = wrapper.getPosition()
                val receiver = Bukkit.getPlayer(receiverUuid) ?: return
                val world = receiver.world
                val particleLoc = Location(world, position.getX(), position.getY(), position.getZ())
                
                val nearbyRestrictedUuids = MythicMobVisibilityService.getNearbyRestricted(particleLoc, 0.5)

                if (nearbyRestrictedUuids.isNotEmpty()) {
                    if (nearbyRestrictedUuids.any { !MythicMobVisibilityService.isVisibleTo(it, receiverUuid) }) {
                        event.isCancelled = true
                    }
                }
            }
        } catch (e: Throwable) {
            logger.log(Level.FINE, "[MythicMobs] Error in packet filter", e)
        }
    }

    private fun checkVisibility(event: PacketSendEvent, entityId: Int, receiverUuid: UUID) {
        if (!MythicMobVisibilityService.isRestricted(entityId)) return
        if (!MythicMobVisibilityService.isVisibleTo(entityId, receiverUuid)) {
            event.isCancelled = true
        }
    }
}

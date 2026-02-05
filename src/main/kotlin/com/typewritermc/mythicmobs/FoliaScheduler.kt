package com.typewritermc.mythicmobs

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.plugin.Plugin
import java.util.logging.Level

/**
 * Lightweight scheduler wrapper that works on both Paper and Folia.
 * Falls back to the classic Bukkit scheduler when Folia APIs are absent.
 * 
 * IMPORTANT for Folia:
 * - Use runSync() for global tasks that DON'T touch entities
 * - Use runAtLocation() for spawning entities at a specific location
 * - Use runAtEntity() for manipulating existing entities
 */
object FoliaScheduler {
    private val plugin: Plugin by lazy { Bukkit.getPluginManager().getPlugin("TypeWriter")!! }
    private val folia: Boolean by lazy { detectFolia() }

    /**
     * Check if the server is running Folia.
     * This uses class existence check for a Folia-specific class that does NOT exist on Paper.
     * Paper 1.21+ exposes the scheduler API methods but does not have the actual
     * threaded-regions implementation classes.
     */
    private fun detectFolia(): Boolean = try {
        // Check for Folia-specific class that only exists on true Folia servers
        // Paper may have scheduler API methods but does NOT have RegionizedServer
        Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
        true
    } catch (_: ClassNotFoundException) {
        false
    } catch (_: Throwable) {
        false
    }

    /** Returns true if running on Folia, false for Paper/Bukkit */
    fun isFolia(): Boolean = folia

    interface SchedulerTask {
        fun cancel()
    }

    /** Wrapper for Folia's ScheduledTask to avoid NoClassDefFoundError with anonymous classes */
    private class FoliaTaskWrapper(private val task: io.papermc.paper.threadedregions.scheduler.ScheduledTask) : SchedulerTask {
        override fun cancel() {
            runCatching { task.cancel() }
        }
    }

    /** Wrapper for Paper's BukkitTask */
    private class PaperTaskWrapper(private val task: org.bukkit.scheduler.BukkitTask) : SchedulerTask {
        override fun cancel() {
            runCatching { task.cancel() }
        }
    }

    /**
     * Run a task on the global region scheduler (Folia) or main thread (Paper).
     * WARNING: Do NOT use this for entity operations on Folia! Use runAtLocation or runAtEntity instead.
     */
    fun runSync(task: () -> Unit) {
        if (!plugin.isEnabled) return
        if (!folia && Bukkit.isPrimaryThread()) {
            runCatching(task)
                .onFailure { throwable -> plugin.logger.log(Level.SEVERE, "[MythicMobs] Sync task failed", throwable) }
            return
        }
        if (folia) {
            Bukkit.getGlobalRegionScheduler().execute(plugin) {
                runCatching(task).onFailure { throwable ->
                    plugin.logger.log(Level.SEVERE, "[MythicMobs] Sync task failed", throwable)
                }
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, Runnable { task() })
        }
    }

    /**
     * Run a task on the region that owns the specified location.
     * Use this for spawning entities or any operation at a specific location.
     */
    fun runAtLocation(location: Location, task: () -> Unit) {
        if (!plugin.isEnabled) return
        if (folia) {
            Bukkit.getRegionScheduler().execute(plugin, location) {
                runCatching(task).onFailure { throwable ->
                    plugin.logger.log(Level.SEVERE, "[MythicMobs] Location task failed", throwable)
                }
            }
        } else {
            if (Bukkit.isPrimaryThread()) {
                runCatching(task)
                    .onFailure { throwable -> plugin.logger.log(Level.SEVERE, "[MythicMobs] Location task failed", throwable) }
            } else {
                Bukkit.getScheduler().runTask(plugin, Runnable { 
                    runCatching(task).onFailure { throwable ->
                        plugin.logger.log(Level.SEVERE, "[MythicMobs] Location task failed", throwable)
                    }
                })
            }
        }
    }

    /**
     * Run a task on the region that owns the specified entity.
     * Use this for any operation on an existing entity.
     */
    fun runAtEntity(entity: Entity, task: () -> Unit) {
        if (!plugin.isEnabled) return
        if (folia) {
            entity.scheduler.execute(plugin, {
                runCatching(task).onFailure { throwable ->
                    plugin.logger.log(Level.SEVERE, "[MythicMobs] Entity task failed", throwable)
                }
            }, null, 0L)
        } else {
            if (Bukkit.isPrimaryThread()) {
                runCatching(task)
                    .onFailure { throwable -> plugin.logger.log(Level.SEVERE, "[MythicMobs] Entity task failed", throwable) }
            } else {
                Bukkit.getScheduler().runTask(plugin, Runnable { 
                    runCatching(task).onFailure { throwable ->
                        plugin.logger.log(Level.SEVERE, "[MythicMobs] Entity task failed", throwable)
                    }
                })
            }
        }
    }

    /**
     * Run a task later on the region that owns the specified location.
     */
    fun runAtLocationLater(location: Location, delayTicks: Long, task: () -> Unit): SchedulerTask? {
        if (!plugin.isEnabled) return null
        return if (folia) {
            val scheduled = Bukkit.getRegionScheduler()
                .runDelayed(plugin, location, { _ -> 
                    runCatching(task).onFailure { throwable ->
                        plugin.logger.log(Level.SEVERE, "[MythicMobs] Delayed location task failed", throwable)
                    }
                }, delayTicks)
            FoliaTaskWrapper(scheduled)
        } else {
            val bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, Runnable { 
                runCatching(task).onFailure { throwable ->
                    plugin.logger.log(Level.SEVERE, "[MythicMobs] Delayed location task failed", throwable)
                }
            }, delayTicks)
            PaperTaskWrapper(bukkitTask)
        }
    }

    /**
     * Run a task later on the entity's scheduler.
     */
    fun runAtEntityLater(entity: Entity, delayTicks: Long, task: () -> Unit): SchedulerTask? {
        if (!plugin.isEnabled) return null
        return if (folia) {
            val scheduled = entity.scheduler.runDelayed(plugin, { _ ->
                runCatching(task).onFailure { throwable ->
                    plugin.logger.log(Level.SEVERE, "[MythicMobs] Delayed entity task failed", throwable)
                }
            }, null, delayTicks)
            if (scheduled != null) FoliaTaskWrapper(scheduled) else null
        } else {
            val bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, Runnable { 
                runCatching(task).onFailure { throwable ->
                    plugin.logger.log(Level.SEVERE, "[MythicMobs] Delayed entity task failed", throwable)
                }
            }, delayTicks)
            PaperTaskWrapper(bukkitTask)
        }
    }

    fun runAsync(task: () -> Unit) {
        if (!plugin.isEnabled) return
        if (folia) {
            Bukkit.getAsyncScheduler().runNow(plugin) { _ ->
                runCatching(task).onFailure { throwable ->
                    plugin.logger.log(Level.SEVERE, "[MythicMobs] Async task failed", throwable)
                }
            }
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable { task() })
        }
    }

    fun runSyncLater(delayTicks: Long, task: () -> Unit): SchedulerTask? {
        if (!plugin.isEnabled) return null
        return if (folia) {
            val scheduled = Bukkit.getGlobalRegionScheduler()
                .runDelayed(plugin, { _ -> task() }, delayTicks)
            FoliaTaskWrapper(scheduled)
        } else {
            val bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, Runnable { task() }, delayTicks)
            PaperTaskWrapper(bukkitTask)
        }
    }

    fun runSyncTimer(delayTicks: Long, periodTicks: Long, task: Runnable): SchedulerTask? {
        if (!plugin.isEnabled) return null
        return if (folia) {
            val scheduled = Bukkit.getGlobalRegionScheduler()
                .runAtFixedRate(plugin, { _ -> task.run() }, delayTicks, periodTicks)
            FoliaTaskWrapper(scheduled)
        } else {
            val bukkitTask =
                Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks)
            PaperTaskWrapper(bukkitTask)
        }
    }

    /**
     * Teleport an entity safely for both Folia and Paper.
     * On Folia, Entity#teleport is BROKEN - must use teleportAsync.
     * On Paper, uses regular synchronous teleport.
     */
    fun teleportSafe(entity: Entity, location: Location, onComplete: (() -> Unit)? = null) {
        if (!entity.isValid) return
        if (folia) {
            // Folia: Must use teleportAsync - synchronous teleport is broken
            entity.teleportAsync(location).thenAccept { success ->
                if (success) {
                    onComplete?.invoke()
                }
            }
        } else {
            // Paper/Bukkit: Synchronous teleport is fine
            entity.teleport(location)
            onComplete?.invoke()
        }
    }
}

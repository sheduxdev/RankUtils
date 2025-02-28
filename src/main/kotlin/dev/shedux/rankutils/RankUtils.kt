package dev.shedux.rankutils

import net.milkbowl.vault.economy.Economy
import net.milkbowl.vault.permission.Permission
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.RegisteredServiceProvider
import org.bukkit.plugin.java.JavaPlugin
import java.text.DecimalFormat
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class RankUtils : JavaPlugin() {

    private var permission: Permission? = null
    private var economy: Economy? = null
    private val cache = ConcurrentHashMap<String, CachedGroupData>()
    private val cacheTimeMs = 5000

    data class CachedGroupData(
        val totalMoney: Double,
        val size: Int,
        val timestamp: Long
    )

    override fun onEnable() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            logger.severe("Vault plugin not found! Disabling RankUtils...")
            server.pluginManager.disablePlugin(this)
            return
        }

        try {
            if (!setupPermissions()) {
                logger.severe("Vault permissions provider not found! Disabling RankUtils...")
                server.pluginManager.disablePlugin(this)
                return
            }
        } catch (e: Exception) {
            logger.severe("Failed to setup permissions: ${e.message}")
            server.pluginManager.disablePlugin(this)
            return
        }

        try {
            if (!setupEconomy()) {
                logger.severe("Vault economy provider not found! Disabling RankUtils...")
                server.pluginManager.disablePlugin(this)
                return
            }
        } catch (e: Exception) {
            logger.severe("Failed to setup economy: ${e.message}")
            server.pluginManager.disablePlugin(this)
            return
        }

        logger.info("Successfully connected to Vault permissions and economy providers")

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                PlaceholderAPIHook(this).register()
                logger.info("Successfully registered PlaceholderAPI integration")
            } catch (e: Exception) {
                logger.warning("Failed to register PlaceholderAPI integration: ${e.message}")
            }
        } else {
            logger.warning("PlaceholderAPI not found - placeholders will not be available")
        }

        server.scheduler.runTaskTimerAsynchronously(this, Runnable {
            updateAllCaches()
        }, 100L, 100L)

        logger.info("RankUtils has been successfully enabled!")
    }

    override fun onDisable() {
        cache.clear()
        logger.info("RankUtils has been disabled")
    }

    private fun setupPermissions(): Boolean {
        val rsp: RegisteredServiceProvider<Permission>? = server.servicesManager.getRegistration(Permission::class.java)
        if (rsp == null) {
            logger.severe("No permission plugin found! Make sure you have a permissions plugin installed (like LuckPerms)")
            return false
        }

        permission = rsp.provider

        if (permission == null) {
            logger.severe("Permission provider is null!")
            return false
        }

        if (permission!!.name.equals("SuperPerms", ignoreCase = true)) {
            logger.severe("SuperPerms is not a supported permissions provider! Disabling RankUtils...")
            return false
        }

        logger.info("Using permissions provider: ${permission!!.name}")
        return true
    }

    private fun setupEconomy(): Boolean {
        val rsp: RegisteredServiceProvider<Economy>? = server.servicesManager.getRegistration(Economy::class.java)
        if (rsp == null) {
            logger.severe("No economy plugin found! Make sure you have an economy plugin installed")
            return false
        }

        economy = rsp.provider
        if (economy == null) {
            logger.severe("Economy provider is null!")
            return false
        }

        logger.info("Using economy provider: ${economy!!.name}")
        return true
    }

    private fun updateAllCaches() {
        try {
            val groups = permission?.groups ?: return
            for (group in groups) {
                updateGroupCache(group)
            }
        } catch (e: Exception) {
            logger.warning("Error updating caches: ${e.message}")
        }
    }

    private fun updateGroupCache(group: String) {
        if (permission == null || economy == null) return

        try {
            server.scheduler.runTaskAsynchronously(this, Runnable {
                try {
                    val players = Bukkit.getOfflinePlayers().filter {
                        try {
                            permission?.playerInGroup(null, it, group) == true
                        } catch (e: Exception) {
                            logger.warning("Error checking if player ${it.name} is in group $group: ${e.message}")
                            false
                        }
                    }

                    val size = players.size
                    val total = players.sumOf {
                        try {
                            economy?.getBalance(it) ?: 0.0
                        } catch (e: Exception) {
                            logger.warning("Error getting balance for player ${it.name}: ${e.message}")
                            0.0
                        }
                    }

                    cache[group] = CachedGroupData(
                        totalMoney = total,
                        size = size,
                        timestamp = System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    logger.warning("Error updating cache for group $group: ${e.message}")
                }
            })
        } catch (e: Exception) {
            logger.warning("Error scheduling cache update for group $group: ${e.message}")
        }
    }

    fun getPlayerPrimaryGroup(player: Player): String? {
        if (permission == null) return null

        return try {
            val groups = permission!!.getPlayerGroups(null, player)
            if (groups.isNotEmpty()) groups[0] else null
        } catch (e: Exception) {
            logger.warning("Error getting primary group for player ${player.name}: ${e.message}")
            null
        }
    }

    fun getTotalMoneyOfGroup(group: String): CompletableFuture<Double> {
        if (permission == null || economy == null) {
            val future = CompletableFuture<Double>()
            future.completeExceptionally(IllegalStateException("Permissions or economy provider not available"))
            return future
        }

        val cachedData = cache[group]
        if (cachedData != null && System.currentTimeMillis() - cachedData.timestamp < cacheTimeMs) {
            return CompletableFuture.completedFuture(cachedData.totalMoney)
        }

        val future = CompletableFuture<Double>()

        server.scheduler.runTaskAsynchronously(this, Runnable {
            try {
                val players = Bukkit.getOfflinePlayers().filter {
                    try {
                        permission?.playerInGroup(null, it, group) == true
                    } catch (e: Exception) {
                        logger.warning("Error checking if player ${it.name} is in group $group: ${e.message}")
                        false
                    }
                }

                val total = players.sumOf {
                    try {
                        economy?.getBalance(it) ?: 0.0
                    } catch (e: Exception) {
                        logger.warning("Error getting balance for player ${it.name}: ${e.message}")
                        0.0
                    }
                }

                cache[group] = CachedGroupData(
                    totalMoney = total,
                    size = players.size,
                    timestamp = System.currentTimeMillis()
                )

                future.complete(total)
            } catch (e: Exception) {
                logger.severe("Error calculating total money for group $group: ${e.message}")
                future.completeExceptionally(e)
            }
        })

        return future
    }

    fun getGroupSize(group: String): CompletableFuture<Int> {
        if (permission == null) {
            val future = CompletableFuture<Int>()
            future.completeExceptionally(IllegalStateException("Permissions provider not available"))
            return future
        }

        val cachedData = cache[group]
        if (cachedData != null && System.currentTimeMillis() - cachedData.timestamp < cacheTimeMs) {
            return CompletableFuture.completedFuture(cachedData.size)
        }

        val future = CompletableFuture<Int>()

        server.scheduler.runTaskAsynchronously(this, Runnable {
            try {
                val players = Bukkit.getOfflinePlayers().filter {
                    try {
                        permission?.playerInGroup(null, it, group) == true
                    } catch (e: Exception) {
                        logger.warning("Error checking if player ${it.name} is in group $group: ${e.message}")
                        false
                    }
                }

                val count = players.size

                if (cachedData == null && economy != null) {
                    val total = players.sumOf {
                        try {
                            economy?.getBalance(it) ?: 0.0
                        } catch (e: Exception) {
                            logger.warning("Error getting balance for player ${it.name}: ${e.message}")
                            0.0
                        }
                    }

                    cache[group] = CachedGroupData(
                        totalMoney = total,
                        size = count,
                        timestamp = System.currentTimeMillis()
                    )
                } else {
                    cache[group] = CachedGroupData(
                        totalMoney = cachedData?.totalMoney ?: 0.0,
                        size = count,
                        timestamp = System.currentTimeMillis()
                    )
                }

                future.complete(count)
            } catch (e: Exception) {
                logger.severe("Error calculating group size for $group: ${e.message}")
                future.completeExceptionally(e)
            }
        })

        return future
    }

    fun formatNumber(number: Double): String {
        return when {
            number >= 1_000_000_000 -> String.format("%.1fB", number / 1_000_000_000)
            number >= 1_000_000 -> String.format("%.1fM", number / 1_000_000)
            number >= 1_000 -> String.format("%.1fK", number / 1_000)
            else -> DecimalFormat("#,###.##").format(number)
        }
    }

    fun formatNumberWithCommas(number: Double): String {
        return DecimalFormat("#,###.##").format(number)
    }
}
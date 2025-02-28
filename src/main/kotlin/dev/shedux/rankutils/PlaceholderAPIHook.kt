package dev.shedux.rankutils

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class PlaceholderAPIHook(private val rankUtils: RankUtils) : PlaceholderExpansion() {

    private val placeholderCache = ConcurrentHashMap<String, CachedValue>()
    private val cacheExpiry = TimeUnit.SECONDS.toMillis(5)

    data class CachedValue(
        val value: String,
        val timestamp: Long
    )

    override fun getIdentifier(): String {
        return "rankutils"
    }

    override fun getAuthor(): String {
        return "sheduxdev"
    }

    override fun getVersion(): String {
        return "1.1"
    }

    override fun persist(): Boolean {
        return true
    }

    override fun canRegister(): Boolean {
        return true
    }

    override fun onRequest(player: OfflinePlayer?, identifier: String): String? {
        val cacheKey = "${player?.uniqueId ?: "server"}_$identifier"
        val cachedValue = placeholderCache[cacheKey]
        if (cachedValue != null && System.currentTimeMillis() - cachedValue.timestamp < cacheExpiry) {
            return cachedValue.value
        }

        if (identifier == "groupsize" && player is Player) {
            val group = rankUtils.getPlayerPrimaryGroup(player)
            if (group != null) {
                return handleGroupSize(player, group, cacheKey)
            }
            return "0"
        }

        if (identifier == "totalmoney" && player is Player) {
            val group = rankUtils.getPlayerPrimaryGroup(player)
            if (group != null) {
                return handleTotalMoney(player, group, cacheKey)
            }
            return "0"
        }

        if (identifier == "totalmoney_formatted" && player is Player) {
            val group = rankUtils.getPlayerPrimaryGroup(player)
            if (group != null) {
                return handleTotalMoneyFormatted(player, group, cacheKey)
            }
            return "0"
        }

        if (identifier == "totalmoney_commas" && player is Player) {
            val group = rankUtils.getPlayerPrimaryGroup(player)
            if (group != null) {
                return handleTotalMoneyCommas(player, group, cacheKey)
            }
            return "0"
        }

        val result = when {
            identifier.startsWith("totalmoney_") -> {
                val group = identifier.removePrefix("totalmoney_")

                if (identifier.endsWith("_formatted")) {
                    val actualGroup = group.removeSuffix("_formatted")
                    handleTotalMoneyFormatted(player, actualGroup, cacheKey)
                } else if (identifier.endsWith("_commas")) {
                    val actualGroup = group.removeSuffix("_commas")
                    handleTotalMoneyCommas(player, actualGroup, cacheKey)
                } else {
                    handleTotalMoney(player, group, cacheKey)
                }
            }
            identifier.startsWith("groupsize_") -> {
                val group = identifier.removePrefix("groupsize_")
                handleGroupSize(player, group, cacheKey)
            }
            identifier == "playergroup" && player is Player -> {
                val group = rankUtils.getPlayerPrimaryGroup(player) ?: "none"
                placeholderCache[cacheKey] = CachedValue(group, System.currentTimeMillis())
                group
            }
            else -> null
        }

        return result
    }

    private fun handleGroupSize(player: OfflinePlayer?, group: String, cacheKey: String): String {
        val future = rankUtils.getGroupSize(group)
        future.thenApply {
            val value = it.toString()
            placeholderCache[cacheKey] = CachedValue(value, System.currentTimeMillis())
        }

        if (future.isDone) {
            return try {
                future.get().toString()
            } catch (e: Exception) {
                "0"
            }
        }

        return placeholderCache[cacheKey]?.value ?: "..."
    }

    private fun handleTotalMoney(player: OfflinePlayer?, group: String, cacheKey: String): String {
        val future = rankUtils.getTotalMoneyOfGroup(group)
        future.thenApply {
            val value = it.toString()
            placeholderCache[cacheKey] = CachedValue(value, System.currentTimeMillis())
        }

        if (future.isDone) {
            return try {
                future.get().toString()
            } catch (e: Exception) {
                "0"
            }
        }

        return placeholderCache[cacheKey]?.value ?: "..."
    }

    private fun handleTotalMoneyFormatted(player: OfflinePlayer?, group: String, cacheKey: String): String {
        val future = rankUtils.getTotalMoneyOfGroup(group)
        future.thenApply {
            val value = rankUtils.formatNumber(it)
            placeholderCache[cacheKey] = CachedValue(value, System.currentTimeMillis())
        }

        if (future.isDone) {
            return try {
                rankUtils.formatNumber(future.get())
            } catch (e: Exception) {
                "0"
            }
        }

        return placeholderCache[cacheKey]?.value ?: "..."
    }

    private fun handleTotalMoneyCommas(player: OfflinePlayer?, group: String, cacheKey: String): String {
        val future = rankUtils.getTotalMoneyOfGroup(group)
        future.thenApply {
            val value = rankUtils.formatNumberWithCommas(it)
            placeholderCache[cacheKey] = CachedValue(value, System.currentTimeMillis())
        }

        if (future.isDone) {
            return try {
                rankUtils.formatNumberWithCommas(future.get())
            } catch (e: Exception) {
                "0"
            }
        }

        return placeholderCache[cacheKey]?.value ?: "..."
    }

    fun cleanupCache() {
        val currentTime = System.currentTimeMillis()
        placeholderCache.entries.removeIf { (_, data) ->
            currentTime - data.timestamp > cacheExpiry
        }
    }
}
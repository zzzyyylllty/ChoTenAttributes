package io.github.zzzyyylllty.attribute.listener

import io.github.zzzyyylllty.attribute.manager.AttributeManager
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit

/**
 * 属性系统监听器
 */
object AttributeListener {

    @SubscribeEvent
    fun onJoin(e: PlayerJoinEvent) {
        // 初始化基础属性当前值
        AttributeManager.initBasicAttributeCurrentForPlayer(e.player.uniqueId)
        // 同步+缓存
        AttributeManager.updatePlayer(e.player, async = false)
    }

    @SubscribeEvent
    fun onRespawn(e: PlayerRespawnEvent) {
    }

    @SubscribeEvent
    fun onQuit(e: PlayerQuitEvent) {
        AttributeManager.clearCache(e.player.uniqueId)
    }

    @SubscribeEvent
    fun onInventoryClose(e: InventoryCloseEvent) {
        val player = e.player
        if (player is Player) {
            AttributeManager.updatePlayer(player)
        }
    }

    @SubscribeEvent
    fun onItemHeld(e: PlayerItemHeldEvent) {
        AttributeManager.updatePlayer(e.player)
    }

    @SubscribeEvent
    fun onSwapHand(e: PlayerSwapHandItemsEvent) {
        AttributeManager.updatePlayer(e.player)
    }

    @SubscribeEvent
    fun onWorldChange(e: PlayerChangedWorldEvent) {
    }
}

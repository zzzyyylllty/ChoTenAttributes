package io.github.zzzyyylllty.attribute.placeholder

import io.github.zzzyyylllty.attribute.ChoTenAttributes
import io.github.zzzyyylllty.attribute.manager.AttributeManager
import io.github.zzzyyylllty.attribute.util.DependencyHelper
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import taboolib.common.platform.function.pluginVersion

class KAngelAttributesExpansion : PlaceholderExpansion() {

    override fun getIdentifier(): String {
        return "kangelattributes"
    }

    override fun getAuthor(): String {
        return "zzzyyylllty"
    }

    override fun getVersion(): String {
        return pluginVersion
    }

    override fun persist(): Boolean {
        return true
    }

    override fun canRegister(): Boolean {
        return DependencyHelper.papi
    }

    override fun onPlaceholderRequest(player: Player, params: String): String? {
        // 处理无需玩家的占位符
        when (params.lowercase()) {
            "version" -> return getVersion()
            "author" -> return getAuthor()
            "identifier" -> return getIdentifier()
        }

        // 玩家相关占位符需要玩家在线
        if (player == null || !player.isOnline) {
            return null
        }
        val onlinePlayer = player.player ?: return null

        // 支持 params 格式:
        // attribute_id -> 返回属性原始值
        // attribute_id_formatted -> 返回格式化后的值（百分比属性添加 % 符号）
        // attribute_id_raw -> 返回原始数值（无格式化）

        val (attributeId, suffix) = parseParams(params)

        // 检查属性是否存在
        if (!AttributeManager.isRegistered(attributeId)) {
            return null
        }

        val value = AttributeManager.getAttributeValue(onlinePlayer, attributeId)

        return when (suffix) {
            "formatted" -> formatValue(attributeId, value)
            else -> value.toString()
        }
    }

    private fun parseParams(params: String): Pair<String, String> {
        // 检查是否以 _formatted 或 _raw 结尾
        return when {
            params.endsWith("_formatted") -> {
                val attributeId = params.removeSuffix("_formatted")
                Pair(attributeId, "formatted")
            }
            params.endsWith("_raw") -> {
                val attributeId = params.removeSuffix("_raw")
                Pair(attributeId, "raw")
            }
            else -> Pair(params, "raw")
        }
    }

    private fun formatValue(attributeId: String, value: Double): String {
        val attribute = AttributeManager.getRegisteredAttributes().find { it.id == attributeId }
        return if (attribute?.isPercentage == true) {
            String.format("%.2f%%", value * 100)
        } else {
            // 根据值大小决定小数位数
            when {
                value == 0.0 -> "0"
                Math.abs(value) < 0.01 -> String.format("%.4f", value)
                Math.abs(value) < 1.0 -> String.format("%.2f", value)
                Math.abs(value) < 1000.0 -> String.format("%.1f", value)
                else -> String.format("%.0f", value)
            }
        }
    }
}
package io.github.zzzyyylllty.attribute.api

import io.github.zzzyyylllty.attribute.data.AttributeModifier
import io.github.zzzyyylllty.attribute.data.AttributeType
import io.github.zzzyyylllty.attribute.data.RegistrationSource
import io.github.zzzyyylllty.attribute.manager.AttributeManager
import org.bukkit.entity.Player
import java.util.UUID

/**
 * 属性系统外部接口
 */
interface ChoTenAttributeAPI {

    /**
     * 获取玩家当前的最终属性值（从缓存读取，性能极高）
     * @param player 玩家
     * @param attributeId 属性 ID (如 "attack_damage")
     * @return 最终数值
     */
    fun getAttributeValue(player: Player, attributeId: String): Double

    /**
     * 为玩家添加一个持久化的属性修饰符（会自动保存到数据库并异步刷新属性）
     * @param uuid 玩家 UUID
     * @param modifier 修饰符对象
     */
    fun addModifier(uuid: UUID, modifier: AttributeModifier)

    /**
     * 移除玩家特定的修饰符
     * @param uuid 玩家 UUID
     * @param modifierUuid 修饰符的唯一标识
     */
    fun removeModifier(uuid: UUID, modifierUuid: UUID)

    /**
     * 强制刷新玩家的属性计算（异步）
     * 通常在装备变动或外部数据变更时调用
     */
    fun updatePlayerAttributes(player: Player)

    /**
     * 获取玩家所有当前的最终属性映射
     */
    fun getAllAttributes(player: Player): Map<String, Double>

    /**
     * 注册一个自定义属性类型
     * @param type 属性定义
     * @return true 注册成功, false 已存在或验证失败
     */
    fun registerAttribute(type: AttributeType): Boolean

    /**
     * 注销一个属性（内置属性不可注销）
     * @param id 属性 ID
     * @return true 移除成功, false 不存在或是内置属性
     */
    fun unregisterAttribute(id: String): Boolean

    /**
     * 查询属性是否已注册
     * @param id 属性 ID
     * @return true 已注册
     */
    fun isRegistered(id: String): Boolean

    /**
     * 获取所有已注册的属性类型
     */
    fun getRegisteredAttributes(): Collection<AttributeType>

    companion object {
        /**
         * 默认 API 实现实例
         */
        @JvmStatic
        val instance: ChoTenAttributeAPI = ChoTenAttributeAPIImpl()
    }
}

/**
 * 接口实现类
 */
class ChoTenAttributeAPIImpl : ChoTenAttributeAPI {

    override fun getAttributeValue(player: Player, attributeId: String): Double {
        return AttributeManager.getAttributeValue(player, attributeId)
    }

    override fun addModifier(uuid: UUID, modifier: AttributeModifier) {
        AttributeManager.addModifier(uuid, modifier)
    }

    override fun removeModifier(uuid: UUID, modifierUuid: UUID) {
        val data = AttributeManager.getPlayerPersistentData(uuid)
        val mods = data.modifiers.toMutableList()
        if (mods.removeIf { it.uuid == modifierUuid }) {
            data.modifiers = mods
            AttributeManager.savePlayerData(data)
            // 尝试更新在线玩家
            org.bukkit.Bukkit.getPlayer(uuid)?.let { AttributeManager.updatePlayer(it) }
        }
    }

    override fun updatePlayerAttributes(player: Player) {
        AttributeManager.updatePlayer(player)
    }

    override fun getAllAttributes(player: Player): Map<String, Double> {
        return AttributeManager.attributeRegistry.keys.associateWith {
            getAttributeValue(player, it)
        }
    }

    override fun registerAttribute(type: AttributeType): Boolean {
        return AttributeManager.registerAttribute(type, RegistrationSource.API)
    }

    override fun unregisterAttribute(id: String): Boolean {
        return AttributeManager.unregisterAttribute(id)
    }

    override fun isRegistered(id: String): Boolean {
        return AttributeManager.isRegistered(id)
    }

    override fun getRegisteredAttributes(): Collection<AttributeType> {
        return AttributeManager.getRegisteredAttributes()
    }
}

package io.github.zzzyyylllty.attribute.data

import taboolib.expansion.Id
import taboolib.expansion.Length
import java.io.Serializable
import java.util.UUID


fun createDefaultPlayerData(uuid: UUID): PlayerAttributeData {
    return PlayerAttributeData(uuid, System.currentTimeMillis(), emptyList())
}

/**
 * 玩家持久化属性数据包装类 (TabooLib PTC DataMapper)
 * 受限于 PTCObj mapper限制，不能有默认参数。
 */
data class PlayerAttributeData(
    @Id
    val uuid: UUID,
    /**
     * 冗余字段，确保主表有可更新列，防止某些数据库驱动在仅更新子表时产生语法错误
     */
    var lastUpdate: Long ,//= System.currentTimeMillis(),
    /**
     * List 字段在 PTC 中会自动创建子表 (player_attribute_data_modifiers)
     * 使用 var 以便在数据变动时重新赋值触发更新
     */
    var modifiers: List<AttributeModifier>// = emptyList()
) : Serializable


/**
 * 属性定义
 */
data class AttributeType(
    val id: String,
    val name: String,
    val vanillaName: String? = null,
    val defaultValue: Double = 0.0,
    val minValue: Double? = null,
    val maxValue: Double? = null,
    val isPercentage: Boolean = false,
    val description: String = "",
    val icon: String? = null
) {
    /**
     * 将数值钳制在 [minValue, maxValue] 范围内
     * 当 min/max 为 null 时不施加对应限制
     */
    fun clamp(value: Double): Double {
        var result = value
        if (minValue != null && result < minValue) result = minValue
        if (maxValue != null && result > maxValue) result = maxValue
        return result
    }
}

/**
 * 属性注册来源
 */
enum class RegistrationSource {
    BUILTIN,
    CONFIG,
    API
}

/**
 * 属性修饰符
 */
data class AttributeModifier(
    @Id
    val uuid: UUID = UUID.randomUUID(),
    @Length(32)
    val attributeId: String,
    val value: Double,
    val type: ModifierType,
    val source: ModifierSource,
    val slot: AttributeSlot = AttributeSlot.OTHER,
    val expireOn: Long? = null
) : Serializable {
    val isExpired: Boolean
        get() = expireOn != null && System.currentTimeMillis() > expireOn
}

/**
 * 修饰符计算类型
 */
enum class ModifierType {
    ADD,
    PERCENT,
    MULTIPLY
}

/**
 * 属性槽位
 */
enum class AttributeSlot {
    HEAD,
    CHEST,
    LEGS,
    FEET,
    INVENTORY,
    MAINHAND,
    OFFHAND,
    OTHER
}

/**
 * 属性来源分类
 */
enum class ModifierSource {
    HAND,
    MAINHAND,
    OFFHAND,
    MELEE,
    RANGED,
    ARMOR,
    SKILL,
    POTION,
    OTHER
}

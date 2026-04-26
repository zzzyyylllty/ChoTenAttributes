package io.github.zzzyyylllty.attribute.listener

import io.github.zzzyyylllty.attribute.ChoTenAttributes
import io.github.zzzyyylllty.attribute.logger.sendStringAsComponent
import io.github.zzzyyylllty.attribute.manager.AttributeManager
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.*
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.platform.util.asLangText
import kotlin.random.Random

/**
 * 攻击类型属性监听器
 * 处理远程攻击、魔法攻击、火焰攻击等属性效果
 */
object AttackListener {

    /**
     * 获取攻击者玩家（提取现有逻辑）
     */
    private fun getAttackerPlayer(damager: Entity): Player? {
        return when (damager) {
            is Player -> damager
            is Projectile -> {
                // 如果是弹射物，检查发射者
                val shooter = damager.shooter
                if (shooter is Player) shooter else null
            }
            // 检查是否为火焰弹、小火球等
            is Fireball -> {
                val shooter = damager.shooter
                if (shooter is Player) shooter else null
            }
            else -> null
        }
    }

    /**
     * 计算伤害加成
     */
    private fun calculateDamageBonus(player: Player, attackTypes: Set<AttackType>, baseDamage: Double): Double {
        var damage = baseDamage

        // 物理伤害加成对所有攻击类型有效
        val physicalBonus = AttributeManager.getAttributeValue(player, "physical_extra_damage")
        if (physicalBonus != 0.0) {
            damage *= (1.0 + physicalBonus).coerceAtLeast(0.0)
        }

        // 远程伤害加成
        if (attackTypes.contains(AttackType.RANGED)) {
            // 新旧属性取最大值
            val rangedBonusNew = AttributeManager.getAttributeValue(player, "ranged_extra_damage")
            val rangedBonusOld = AttributeManager.getAttributeValue(player, "ranged_attack_damage")
            val rangedBonus = maxOf(rangedBonusNew, rangedBonusOld)
            if (rangedBonus != 0.0) {
                damage *= (1.0 + rangedBonus).coerceAtLeast(0.0)
            }
        }

        // 魔法伤害加成
        if (attackTypes.contains(AttackType.MAGIC)) {
            // 新旧属性取最大值
            val magicBonusNew = AttributeManager.getAttributeValue(player, "magic_extra_damage")
            val magicBonusOld = AttributeManager.getAttributeValue(player, "magic_attack_damage")
            val magicBonus = maxOf(magicBonusNew, magicBonusOld)
            if (magicBonus != 0.0) {
                damage *= (1.0 + magicBonus).coerceAtLeast(0.0)
            }
        }

        // 火焰伤害加成（如果攻击是火焰类型）
        if (attackTypes.contains(AttackType.FIRE)) {
            // 新旧属性取最大值
            val fireBonusNew = AttributeManager.getAttributeValue(player, "fire_extra_damage")
            val fireBonusOld = AttributeManager.getAttributeValue(player, "fire_attack_damage")
            val fireBonus = maxOf(fireBonusNew, fireBonusOld)
            if (fireBonus != 0.0) {
                damage *= (1.0 + fireBonus).coerceAtLeast(0.0)
            }
        }

        return damage
    }

    /**
     * 计算伤害减免
     */
    private fun calculateDamageReduction(victim: Player, attackTypes: Set<AttackType>, damage: Double): Double {
        var remainingDamage = damage

        // 全伤害减免对所有攻击类型有效
        val allReduction = AttributeManager.getAttributeValue(victim, "all_damage_reduction")
        if (allReduction > 0) {
            val reduction = allReduction.coerceAtMost(1.0) // 限制在100%以内
            remainingDamage *= (1.0 - reduction)
        }

        // 物理伤害减免（包含近战和远程物理伤害，但不包含火焰伤害）
        if ((attackTypes.contains(AttackType.MELEE) || attackTypes.contains(AttackType.RANGED)) && !attackTypes.contains(AttackType.FIRE)) {
            val physicalReduction = AttributeManager.getAttributeValue(victim, "physical_damage_reduction")
            if (physicalReduction > 0) {
                val reduction = physicalReduction.coerceAtMost(1.0)
                remainingDamage *= (1.0 - reduction)
            }
        }

        // 远程伤害减免
        if (attackTypes.contains(AttackType.RANGED)) {
            val rangedReduction = AttributeManager.getAttributeValue(victim, "ranged_damage_reduction")
            if (rangedReduction > 0) {
                val reduction = rangedReduction.coerceAtMost(1.0)
                remainingDamage *= (1.0 - reduction)
            }
        }

        // 魔法伤害减免
        if (attackTypes.contains(AttackType.MAGIC)) {
            val magicReduction = AttributeManager.getAttributeValue(victim, "magic_damage_reduction")
            if (magicReduction > 0) {
                val reduction = magicReduction.coerceAtMost(1.0)
                remainingDamage *= (1.0 - reduction)
            }
        }

        // 火焰伤害减免
        if (attackTypes.contains(AttackType.FIRE)) {
            val fireReduction = AttributeManager.getAttributeValue(victim, "fire_damage_reduction")
            if (fireReduction > 0) {
                val reduction = fireReduction.coerceAtMost(1.0)
                remainingDamage *= (1.0 - reduction)
            }
        }

        // 确保伤害不为负数
        return remainingDamage.coerceAtLeast(0.0)
    }

    /**
     * 处理实体伤害事件，应用攻击类型属性加成
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        val victim = event.entity

        // 1. 获取攻击者玩家
        val attacker = getAttackerPlayer(damager)
        if (attacker == null || victim.isDead) return

        val originalDamage = event.damage
        var modifiedDamage = originalDamage

        // 2. 检测攻击类型
        val attackTypes = detectAttackTypes(damager, attacker).toMutableSet()

        // 2.1 检查事件原因是否为火焰伤害
        val isFireDamage = attackTypes.contains(AttackType.FIRE) ||
                          event.cause.name.contains("FIRE", ignoreCase = true) ||
                          event.cause.name.contains("LAVA", ignoreCase = true) ||
                          event.cause.name.contains("HOT_FLOOR", ignoreCase = true) ||
                          event.cause.name.contains("MELTING", ignoreCase = true)
        if (isFireDamage) {
            attackTypes.add(AttackType.FIRE)
        }

        // 3. 计算伤害加成（攻击者）
        modifiedDamage = calculateDamageBonus(attacker, attackTypes, modifiedDamage)

        // 4. 计算伤害减免（受害者，如果是玩家）
        if (victim is Player) {
            modifiedDamage = calculateDamageReduction(victim, attackTypes, modifiedDamage)
        }

        // 5. 火焰攻击几率逻辑（现有逻辑保持不变）
        // 注意：火焰伤害加成已经在calculateDamageBonus中处理，但火焰攻击几率可能触发额外加成
        // 应用火焰攻击几率（对非火焰攻击有一定几率附加火焰效果）
        if (!isFireDamage && victim is LivingEntity) {
            val fireChance = AttributeManager.getAttributeValue(attacker, "fire_attack_chance")
            if (fireChance > 0 && Random.nextDouble() < fireChance) {
                // 附加火焰效果
                victim.fireTicks = 100 // 5秒燃烧时间

                // 如果是火焰攻击几率触发的火焰效果，也应用火焰攻击伤害加成
                // 这里需要应用火焰伤害加成，因为攻击现在变成了火焰类型
                val fireBonusNew = AttributeManager.getAttributeValue(attacker, "fire_extra_damage")
                val fireBonusOld = AttributeManager.getAttributeValue(attacker, "fire_attack_damage")
                val fireBonus = maxOf(fireBonusNew, fireBonusOld)
                if (fireBonus != 0.0) {
                    modifiedDamage *= (1.0 + fireBonus).coerceAtLeast(0.0)
                }

                // 如果受害者是玩家，应用火焰伤害减免（因为攻击现在变成了火焰类型）
                if (victim is Player) {
                    val fireReduction = AttributeManager.getAttributeValue(victim, "fire_damage_reduction")
                    if (fireReduction > 0) {
                        val reduction = fireReduction.coerceAtMost(1.0)
                        modifiedDamage *= (1.0 - reduction)
                    }
                    // 注意：全伤害减免已经在前面的calculateDamageReduction中应用
                    // 不需要重复应用
                }
            }
        }

        // 6. 应用固定值魔法伤害（现有逻辑）
        val magicDamage = AttributeManager.getAttributeValue(attacker, "magic_damage")
        if (magicDamage > 0) {
            modifiedDamage += magicDamage
        }

        // 确保伤害不为负数
        modifiedDamage = modifiedDamage.coerceAtLeast(0.0)

        // 7. 闪避与格挡判定（受害者）
        if (victim is Player) {
            val bdEnabled = ChoTenAttributes.config.getBoolean("block-dodge-settings.enabled", true)
            if (bdEnabled) {
                // 闪避判定：完全取消伤害
                val dodgeChance = AttributeManager.getAttributeValue(victim, "dodge_chance")
                if (dodgeChance > 0 && Random.nextDouble() < dodgeChance) {
                    modifiedDamage = 0.0
                    victim.sendStringAsComponent(victim.asLangText("Dodge_Triggered"))
                } else {
                    // 格挡判定：抵消部分伤害
                    val blockChance = AttributeManager.getAttributeValue(victim, "block_chance")
                    if (blockChance > 0 && Random.nextDouble() < blockChance) {
                        val blockDefense = AttributeManager.getAttributeValue(victim, "block_defense")
                        val blockDefensePercent = AttributeManager.getAttributeValue(victim, "block_defense_percent")
                        val blocked = blockDefense + modifiedDamage * blockDefensePercent
                        modifiedDamage = (modifiedDamage - blocked).coerceAtLeast(0.0)
                        victim.sendStringAsComponent(victim.asLangText("Block_Triggered"))
                    }
                }
            }
        }

        // 8. 更新伤害值
        if (modifiedDamage != originalDamage) {
            event.damage = modifiedDamage
        }
    }

    /**
     * 检测攻击类型（可能同时属于多种类型）
     */
    private fun detectAttackTypes(damager: Entity, player: Player): Set<AttackType> {
        val types = mutableSetOf<AttackType>()

        // 1. 检查是否为远程攻击（弹射物）
        if (damager is Projectile) {
            types.add(AttackType.RANGED)
        }

        // 2. 检查是否为火焰攻击（火焰弹、小火球等）
        if (damager is Fireball) {
            types.add(AttackType.FIRE)
        }

        // 3. 检查武器是否有火焰附加附魔
        val item = player.inventory.itemInMainHand
        if (item.hasItemMeta() && hasFireAspectEnchantment(item)) {
            types.add(AttackType.FIRE)
        }

        // 4. 检查是否为魔法攻击
        if (isMagicAttack(player)) {
            types.add(AttackType.MAGIC)
        }

        // 5. 如果没有特殊类型，则视为近战攻击
        if (types.isEmpty() && damager is Player) {
            types.add(AttackType.MELEE)
        }

        return types
    }

    /**
     * 检测是否为魔法攻击
     * 基于玩家的magic_damage属性值判断
     */
    private fun isMagicAttack(player: Player): Boolean {
        // 检查玩家是否有魔法伤害属性
        val magicDamage = AttributeManager.getAttributeValue(player, "magic_damage")
        return magicDamage > 0
    }

    /**
     * 检查物品是否有火焰附加附魔
     */
    private fun hasFireAspectEnchantment(item: ItemStack): Boolean {
        if (!item.hasItemMeta()) return false

        val meta = item.itemMeta!!
        // 检查所有附魔，查找名称中包含"fire"的
        for (enchant in meta.enchants.keys) {
            val key = enchant.key
            if (key.key.contains("fire", ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    /**
     * 攻击类型枚举
     */
    private enum class AttackType {
        MELEE,      // 近战攻击
        RANGED,     // 远程攻击
        MAGIC,      // 魔法攻击
        FIRE        // 火焰攻击
    }
}
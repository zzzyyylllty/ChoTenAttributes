package io.github.zzzyyylllty.attribute.manager

import io.github.zzzyyylllty.attribute.data.AttributeModifier
import io.github.zzzyyylllty.attribute.data.AttributeType
import io.github.zzzyyylllty.attribute.data.ModifierType
import io.github.zzzyyylllty.attribute.data.PlayerAttributeData
import io.github.zzzyyylllty.attribute.data.createDefaultPlayerData
import io.github.zzzyyylllty.attribute.data.*
import io.github.zzzyyylllty.attribute.event.ChoTenAttributeRegisterEvent
import io.github.zzzyyylllty.attribute.logger.warningS
import io.github.zzzyyylllty.attribute.util.devLog
import io.github.zzzyyylllty.attribute.util.jsonUtils
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.attribute.AttributeModifier as BukkitAttributeModifier
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.submit
import taboolib.expansion.CustomType
import taboolib.expansion.dbFile
import taboolib.expansion.mapper
import taboolib.module.database.ColumnTypePostgreSQL
import taboolib.module.database.ColumnTypeSQL
import taboolib.module.database.ColumnTypeSQLite
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs


object AttributeModifierType : CustomType {
    override val type = AttributeModifier::class.java
    override val typeSQL = ColumnTypeSQL.LONGTEXT
    override val typeSQLite = ColumnTypeSQLite.TEXT
    override val typePostgreSQL = ColumnTypePostgreSQL.TEXT
    override val length = 6

    override fun serialize(value: Any) = jsonUtils.toJson(value as AttributeModifier, AttributeModifier::class.java)
    override fun deserialize(value: Any): AttributeModifier {
        val raw = value.toString()
        // If the string is double-encoded JSON (starts and ends with quotes), parse as JsonElement first to unwrap
        val jsonStr = if (raw.startsWith("\"") && raw.endsWith("\"")) {
            jsonUtils.fromJson(raw, String::class.java) ?: raw
        } else {
            raw
        }
        // Guard: if the string doesn't look like a JSON object, return a placeholder
        if (!jsonStr.trimStart().startsWith("{")) {
            warningS("AttributeModifierType: cannot deserialize non-JSON value: $jsonStr")
            return AttributeModifier(
                attributeId = "unknown",
                value = 0.0,
                type = ModifierType.ADD,
                source = ModifierSource.OTHER
            )
        }
        return jsonUtils.fromJson(jsonStr, AttributeModifier::class.java)
    }
}

object PlayerAttributeDataType : CustomType {
    override val type = PlayerAttributeData::class.java
    override val typeSQL = ColumnTypeSQL.LONGTEXT
    override val typeSQLite = ColumnTypeSQLite.TEXT
    override val typePostgreSQL = ColumnTypePostgreSQL.TEXT
    override val length = 6

    override fun serialize(value: Any) = jsonUtils.toJson(value as PlayerAttributeData, PlayerAttributeData::class.java)
    override fun deserialize(value: Any): PlayerAttributeData {
        val raw = value.toString()
        val jsonStr = if (raw.startsWith("\"") && raw.endsWith("\"")) {
            jsonUtils.fromJson(raw, String::class.java) ?: raw
        } else {
            raw
        }
        if (!jsonStr.trimStart().startsWith("{")) {
            warningS("PlayerAttributeDataType: cannot deserialize non-JSON value: $jsonStr")
            throw IllegalStateException("PlayerAttributeDataType: invalid JSON: $jsonStr")
        }
        return jsonUtils.fromJson(jsonStr, PlayerAttributeData::class.java)
    }
}

// tmd，改了自动注册，注释掉这段
//@Awake(LifeCycle.INIT)
//fun registerCustomTypeFactory() {
//// 注册
//    CustomTypeFactory.reg.(DateType)
//}

object AttributeManager {

    val dataMapper by mapper<PlayerAttributeData>(dbFile("attributes.db"))

    // 缓存最终计算结果。UUID -> (AttributeID -> Value)
    private val attributeCache = ConcurrentHashMap<UUID, Map<String, Double>>()
    // 缓存每个玩家上次同步的值，用于增量更新。UUID -> (AttributeID -> Value)
    private val lastSyncedValues = ConcurrentHashMap<UUID, Map<String, Double>>()
    // 缓存 vanillaName 到 Bukkit Attribute 实例的映射（可能为 null 表示找不到）
    private val vanillaAttributeCache = ConcurrentHashMap<String, org.bukkit.attribute.Attribute?>()

    // 属性注册表
    val attributeRegistry = ConcurrentHashMap<String, AttributeType>()

    // 内置属性 ID 集合（不可被注销）
    private val builtinAttributeIds = mutableSetOf<String>()

    // 基础值属性 ID 集合（从 basic-attributes 配置加载）
    private val basicAttributeIds = mutableSetOf<String>()

    // ID 合法性校验：仅允许小写字母、数字、下划线
    private val VALID_ID_PATTERN = Regex("^[a-z0-9_]+$")

    /**
     * 注册内置属性，插件启动时调用一次
     * 覆盖 Minecraft 全部 11 个原生 Attribute (RegistryKey<Attribute>)
     */
    fun registerBuiltinAttributes() {
        val builtins = listOf(
            AttributeType("max_health", "生命上限", "max_health", 20.0),
            AttributeType("max_absorption", "伤害吸收上限", "max_absorption", 0.0),
            AttributeType("movement_speed", "移动速度", "movement_speed", 0.1),
            AttributeType("attack_damage", "攻击力", "attack_damage", 1.0),
            AttributeType("attack_speed", "攻击速度", "attack_speed", 4.0),
            AttributeType("armor", "护甲值", "armor", 0.0),
            AttributeType("armor_toughness", "护甲韧性", "armor_toughness", 0.0),
            AttributeType("attack_knockback", "攻击击退", "attack_knockback", 0.0),
            AttributeType("knockback_resistance", "击退抗性", "knockback_resistance", 0.0),
            AttributeType("luck", "幸运", "luck", 0.0),
            AttributeType("fall_damage_multiplier", "跌落伤害倍率", "fall_damage_multiplier", 1.0),
            AttributeType("burning_time", "燃烧时间", "burning_time", 1.0),
            AttributeType("explosion_knockback_resistance", "爆炸击退抗性", "explosion_knockback_resistance", 0.0),
            AttributeType("mining_efficiency", "挖掘效率", "mining_efficiency", 0.0),
            AttributeType("movement_efficiency", "移动效率", "movement_efficiency", 0.0),
            AttributeType("oxygen_bonus", "氧气加成", "oxygen_bonus", 0.0),
            AttributeType("player_block_break_speed", "方块破坏速度", "player_block_break_speed", 1.0),
            AttributeType("player_block_interaction_range", "方块交互距离", "player_block_interaction_range", 4.5),
            AttributeType("player_entity_interaction_range", "实体交互距离", "player_entity_interaction_range", 3.0),
            AttributeType("player_submerged_mining_speed", "水下挖掘速度", "player_submerged_mining_speed", 0.2),
            AttributeType("player_mining_speed", "挖掘速度", "player_mining_speed", 1.0),
            AttributeType("gravity", "重力", "gravity", 0.08),
            AttributeType("safe_fall_distance", "安全跌落距离", "safe_fall_distance", 3.0),
            AttributeType("scale", "缩放", "scale", 1.0),
            AttributeType("step_height", "台阶高度", "step_height", 0.6),
            AttributeType("spawn_reinforcements", "僵尸增援概率", "spawn_reinforcements", 0.0),
            // 自定义扩展属性 (无 vanillaName 映射)
            AttributeType("defense", "综合防御", null, 0.0),
            AttributeType("crit_chance", "暴击率", null, 0.0, isPercentage = true),
            AttributeType("crit_damage", "暴击伤害", null, 0.0, isPercentage = true),
            AttributeType("lifesteal", "生命偷取", null, 0.0, isPercentage = true),
            AttributeType("dodge_chance", "闪避率", null, 0.0, isPercentage = true),
            // 格挡相关属性
            AttributeType("block_chance", "格挡概率", null, 0.0, isPercentage = true),
            AttributeType("block_defense", "格挡防御", null, 0.0, isPercentage = false),
            AttributeType("block_defense_percent", "格挡防御百分比", null, 0.0, isPercentage = true),
            // 新增攻击类型属性
            AttributeType("ranged_attack_damage", "远程攻击伤害", null, 0.0, isPercentage = true),
            AttributeType("magic_attack_damage", "魔法攻击伤害", null, 0.0, isPercentage = true),
            AttributeType("magic_damage", "魔法伤害", null, 0.0, isPercentage = false),
            AttributeType("fire_attack_damage", "火焰攻击伤害", null, 0.0, isPercentage = true),
            AttributeType("fire_attack_chance", "火焰攻击几率", null, 0.0, isPercentage = true),
            // 伤害增益属性（攻击者）
            AttributeType("physical_extra_damage", "物理伤害加成", null, 0.0, isPercentage = true),
            AttributeType("ranged_extra_damage", "远程伤害加成", null, 0.0, isPercentage = true),
            AttributeType("magic_extra_damage", "魔法伤害加成", null, 0.0, isPercentage = true),
            AttributeType("fire_extra_damage", "火焰伤害加成", null, 0.0, isPercentage = true),
            // 伤害抗性属性（受害者）
            AttributeType("physical_damage_reduction", "物理伤害减免", null, 0.0, isPercentage = true),
            AttributeType("ranged_damage_reduction", "远程伤害减免", null, 0.0, isPercentage = true),
            AttributeType("magic_damage_reduction", "魔法伤害减免", null, 0.0, isPercentage = true),
            AttributeType("fire_damage_reduction", "火焰伤害减免", null, 0.0, isPercentage = true),
            AttributeType("all_damage_reduction", "全伤害减免", null, 0.0, isPercentage = true),
        )
        for (type in builtins) {
            registerAttribute(type, RegistrationSource.BUILTIN)
            builtinAttributeIds.add(type.id)
        }
    }

    /**
     * 注册一个属性类型
     * @return true 注册成功, false 已存在或验证失败
     */
    fun registerAttribute(type: AttributeType, source: RegistrationSource = RegistrationSource.API): Boolean {
        // 验证 id 非空且合法
        if (type.id.isBlank() || !VALID_ID_PATTERN.matches(type.id)) {
            warningS("AttributeManager: invalid attribute id '${type.id}' (only [a-z0-9_] allowed)")
            return false
        }
        // 检查是否已存在
        if (attributeRegistry.containsKey(type.id)) {
            return false
        }
        // 验证 vanillaName（若非 null）
        if (type.vanillaName != null) {
            try {
                var key = NamespacedKey.minecraft(type.vanillaName)
                var attr = Registry.ATTRIBUTE.get(key)
                // 如果原始名称未找到且不包含点号，尝试添加 generic. 前缀
                if (attr == null && !type.vanillaName.contains('.')) {
                    key = NamespacedKey.minecraft("generic.${type.vanillaName}")
                    attr = Registry.ATTRIBUTE.get(key)
                }
                if (attr == null) {
                    warningS("AttributeManager: vanillaName '${type.vanillaName}' not found in Registry.ATTRIBUTE (tried '${type.vanillaName}' and 'generic.${type.vanillaName}')")
                    return false
                }
                // 验证成功，将找到的属性实例存入缓存，避免后续重复查找
                vanillaAttributeCache[type.vanillaName] = attr
            } catch (e: Exception) {
                warningS("AttributeManager: invalid vanillaName '${type.vanillaName}': ${e.message}")
                return false
            }
        }
        attributeRegistry[type.id] = type
        // 事件
        ChoTenAttributeRegisterEvent(type, source).call()
        return true
    }

    /**
     * 注销一个属性
     * @return true 移除成功, false 不存在或是内置属性
     */
    fun unregisterAttribute(id: String): Boolean {
        if (id in builtinAttributeIds) {
            warningS("AttributeManager: cannot unregister builtin attribute '$id'")
            return false
        }
        val removed = attributeRegistry.remove(id) != null
        if (removed) {
            // 清理所有在线玩家缓存中该属性的值
            for (uuid in attributeCache.keys) {
                attributeCache.computeIfPresent(uuid) { _, cache ->
                    cache.toMutableMap().apply { remove(id) }
                }
            }
        }
        return removed
    }

    /**
     * 查询属性是否已注册
     */
    fun isRegistered(id: String): Boolean {
        return attributeRegistry.containsKey(id)
    }

    /**
     * 获取所有已注册的属性
     */
    fun getRegisteredAttributes(): Collection<AttributeType> {
        return attributeRegistry.values.toList()
    }

    /**
     * 从配置文件加载属性定义
     * 检查 getDataFolder()/attributes 目录，不存在则从 resources 释放默认文件
     */
    fun loadAttributeFiles() {
        val plugin = io.github.zzzyyylllty.attribute.ChoTenAttributes
        val dataFolder = taboolib.common.platform.function.getDataFolder()
        val attrDir = java.io.File(dataFolder, "attributes")

        if (!attrDir.exists()) {
            attrDir.mkdirs()
            // 从 jar 释放默认配置文件
            val defaultResource = plugin::class.java.classLoader.getResource("attributes/default.yml")
            if (defaultResource != null) {
                val targetFile = java.io.File(attrDir, "default.yml")
                defaultResource.openStream().use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                devLog(("AttributeManager: released default attribute config to ${targetFile.path}"))
            }
        }

        val files = attrDir.listFiles { f -> f.extension in listOf("yml", "yaml") } ?: return
        for (file in files) {
            loadAttributeFile(file)
        }
    }

    /**
     * 加载单个属性配置文件
     */
    private fun loadAttributeFile(file: java.io.File) {
        try {
            val config = taboolib.module.configuration.Configuration.loadFromFile(file)
            val registered = mutableListOf<String>()
            val skipped = mutableListOf<String>()

            for (id in config.getKeys(false)) {
                val section = config.getConfigurationSection(id) ?: continue
                val name = section.getString("name", id) ?: id
                val vanillaName = section.getString("vanillaName")
                val defaultValue = section.getDouble("defaultValue", 0.0)
                val minValue = if (section.contains("minValue")) section.getDouble("minValue") else null
                val maxValue = if (section.contains("maxValue")) section.getDouble("maxValue") else null
                val isPercentage = section.getBoolean("isPercentage", false)
                val description = section.getString("description", "") ?: ""
                val icon = section.getString("icon")

                val type = AttributeType(
                    id = id,
                    name = name,
                    vanillaName = vanillaName,
                    defaultValue = defaultValue,
                    minValue = minValue,
                    maxValue = maxValue,
                    isPercentage = isPercentage,
                    description = description,
                    icon = icon
                )

                if (registerAttribute(type, RegistrationSource.CONFIG)) {
                    registered.add(id)
                } else {
                    skipped.add(id)
                }
            }

            if (registered.isNotEmpty()) {
                devLog("AttributeManager: loaded ${registered.size} attribute(s) from ${file.name}: ${registered.joinToString()}")
            }
            if (skipped.isNotEmpty()) {
                warningS("AttributeManager: skipped ${skipped.size} attribute(s) from ${file.name}: ${skipped.joinToString()}")
            }
        } catch (e: Exception) {
            warningS("AttributeManager: failed to load attribute file ${file.name}: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 从主配置加载基础值属性（basic-attributes）
     */
    fun loadBasicAttributes() {
        val config = io.github.zzzyyylllty.attribute.ChoTenAttributes.config
        val section = config.getConfigurationSection("basic-attributes") ?: return
        val registered = mutableListOf<String>()
        val skipped = mutableListOf<String>()

        // 清空基础属性ID集合
        basicAttributeIds.clear()

        for (id in section.getKeys(false)) {
            val attrSection = section.getConfigurationSection(id) ?: continue
            val defaultSection = attrSection.getConfigurationSection("default") ?: continue

            val min = defaultSection.getDouble("min", 0.0)
            val max = defaultSection.getDouble("max", 100.0)
            val regeneration = defaultSection.getDouble("regeneration", 0.0)

            // 添加到基础属性ID集合
            basicAttributeIds.add(id)

            // 注册四个衍生属性
            val minAttr = AttributeType(
                id = "${id}_min",
                name = "${id}最小值",
                vanillaName = null,
                defaultValue = min,
                isPercentage = false,
                description = "${id}最小值"
            )
            val maxAttr = AttributeType(
                id = "${id}_max",
                name = "${id}最大值",
                vanillaName = null,
                defaultValue = max,
                isPercentage = false,
                description = "${id}最大值"
            )
            val regenAttr = AttributeType(
                id = "${id}_regeneration",
                name = "${id}恢复速度",
                vanillaName = null,
                defaultValue = regeneration,
                isPercentage = false,
                description = "${id}每秒恢复速度"
            )
            val currentAttr = AttributeType(
                id = "${id}_current",
                name = "${id}当前值",
                vanillaName = null,
                defaultValue = 0.0, // 基础值为0，当前值由修饰符提供
                isPercentage = false,
                description = "${id}当前值"
            )

            if (registerAttribute(minAttr, RegistrationSource.CONFIG)) {
                registered.add(minAttr.id)
            } else {
                skipped.add(minAttr.id)
            }
            if (registerAttribute(maxAttr, RegistrationSource.CONFIG)) {
                registered.add(maxAttr.id)
            } else {
                skipped.add(maxAttr.id)
            }
            if (registerAttribute(regenAttr, RegistrationSource.CONFIG)) {
                registered.add(regenAttr.id)
            } else {
                skipped.add(regenAttr.id)
            }
            if (registerAttribute(currentAttr, RegistrationSource.CONFIG)) {
                registered.add(currentAttr.id)
            } else {
                skipped.add(currentAttr.id)
            }
        }

        if (registered.isNotEmpty()) {
            devLog("AttributeManager: loaded ${registered.size} basic attribute(s) from config: ${registered.joinToString()}")
        }
        if (skipped.isNotEmpty()) {
            warningS("AttributeManager: skipped ${skipped.size} basic attribute(s) from config: ${skipped.joinToString()}")
        }
    }

    /**
     * 初始化玩家的基础属性当前值修饰符
     * 如果玩家没有当前值修饰符，则创建一个，初始值为最大值
     */
    fun initBasicAttributeCurrentForPlayer(uuid: UUID) {
        if (basicAttributeIds.isEmpty()) return
        val data = getPlayerPersistentData(uuid)
        val modifiers = data.modifiers.toMutableList()
        var modified = false

        for (basicId in basicAttributeIds) {
            val currentAttrId = "${basicId}_current"
            // 检查是否已存在当前值修饰符（来源为 OTHER）
            val existing = modifiers.find { it.attributeId == currentAttrId && it.source == ModifierSource.OTHER }
            if (existing == null) {
                // 获取最大值属性
                val maxAttrId = "${basicId}_max"
                val maxValue = attributeRegistry[maxAttrId]?.defaultValue ?: 100.0
                // 创建当前值修饰符
                val currentModifier = AttributeModifier(
                    attributeId = currentAttrId,
                    value = maxValue,
                    type = ModifierType.ADD,
                    source = ModifierSource.OTHER
                )
                modifiers.add(currentModifier)
                modified = true
                devLog("AttributeManager: added current modifier for $basicId = $maxValue for player $uuid")
            }
        }

        if (modified) {
            data.modifiers = modifiers
            savePlayerData(data)
        }
    }

    /**
     * 更新所有在线玩家的基础属性恢复
     * 每秒调用一次
     */
    fun updateRegeneration() {
        if (basicAttributeIds.isEmpty()) return
        for (player in org.bukkit.Bukkit.getOnlinePlayers()) {
            val uuid = player.uniqueId
            val data = getPlayerPersistentData(uuid)
            val modifiers = data.modifiers.toMutableList()
            var modified = false

            for (basicId in basicAttributeIds) {
                val currentAttrId = "${basicId}_current"
                val minAttrId = "${basicId}_min"
                val maxAttrId = "${basicId}_max"
                val regenAttrId = "${basicId}_regeneration"

                val min = attributeRegistry[minAttrId]?.defaultValue ?: 0.0
                val max = attributeRegistry[maxAttrId]?.defaultValue ?: 100.0
                val regen = attributeRegistry[regenAttrId]?.defaultValue ?: 0.0

                // 找到当前值修饰符
                val existingIndex = modifiers.indexOfFirst { it.attributeId == currentAttrId && it.source == ModifierSource.OTHER }
                if (existingIndex == -1) continue
                val existing = modifiers[existingIndex]
                var newValue = existing.value + regen
                // 限制在最小值和最大值之间
                if (newValue > max) newValue = max
                if (newValue < min) newValue = min
                // 如果值有变化，更新修饰符
                if (newValue != existing.value) {
                    val newModifier = existing.copy(value = newValue)
                    modifiers[existingIndex] = newModifier
                    modified = true
                    devLog("AttributeManager: updated $currentAttrId from ${existing.value} to $newValue for player ${player.name}")
                }
            }

            if (modified) {
                data.modifiers = modifiers
                savePlayerData(data)
                // 更新玩家属性缓存
                updatePlayer(player, async = true)
            }
        }
    }


    @Awake(LifeCycle.ACTIVE)
    fun startRegenerationTask() {
        submit(async = true, period = 20L) { // 20 ticks = 1 second
            updateRegeneration()
        }
        devLog("AttributeManager: started regeneration task")
    }

    /**
     * 重载属性：保留内置属性，移除所有非内置属性并重新从配置加载
     */
    fun reloadAttributes() {
        // 移除非内置属性
        val toRemove = attributeRegistry.keys.filter { it !in builtinAttributeIds }
        for (id in toRemove) {
            attributeRegistry.remove(id)
            // 清理在线玩家缓存
            for (uuid in attributeCache.keys) {
                attributeCache.computeIfPresent(uuid) { _, cache ->
                    cache.toMutableMap().apply { remove(id) }
                }
            }
        }
        // 重新从配置加载
        loadAttributeFiles()
        loadBasicAttributes()
        // 刷新所有在线玩家
        for (player in org.bukkit.Bukkit.getOnlinePlayers()) {
            updatePlayer(player)
        }
    }

    /**
     * 获取玩家数据
     */
    fun getPlayerPersistentData(uuid: UUID): PlayerAttributeData {
        val a1aa = dataMapper.findById(uuid)
        return a1aa ?: createDefaultPlayerData(uuid)
    }

    /**
     * 保存玩家数据
     */
    fun savePlayerData(data: PlayerAttributeData) {
        data.lastUpdate = System.currentTimeMillis()
        dataMapper.insertOrUpdate(data) { "uuid" eq data.uuid }
    }

    /**
     * 更新玩家属性
     */
    fun updatePlayer(player: Player, async: Boolean = true) {
        if (async) {
            submit(async = true) {
                doUpdate(player)
            }
        } else {
            doUpdate(player)
        }
    }

    private fun doUpdate(player: Player) {
        val uuid = player.uniqueId
        val data = getPlayerPersistentData(uuid)

        // DEBUG
        devLog(("Updating player $uuid, raw modifiers size: ${data.modifiers.size}"))
        data.modifiers.forEachIndexed { index, any ->
            devLog(("  - Modifier[$index]: class=${any?.javaClass?.name}, value=$any"))
        }

        // 1. 处理过期修饰符
        val mods = data.modifiers.toMutableList()
        val expiredRemoved = mods.removeIf { it.isExpired }
        if (expiredRemoved) {
            data.modifiers = mods
            savePlayerData(data)
        }

        // 2. 汇总所有修饰符
        val allModifiers = mutableListOf<AttributeModifier>()
        allModifiers.addAll(mods)
        allModifiers.addAll(getEquipmentModifiers(player))

        // 3. 计算最终值
        val finalValues = mutableMapOf<String, Double>()
        attributeRegistry.values.forEach { type ->
            val modsForAttr = allModifiers.filter { it.attributeId == type.id }
            if (modsForAttr.isNotEmpty() || type.defaultValue != 0.0) {
                val calculated = calculateFinal(type.defaultValue, modsForAttr)
                val clamped = type.clamp(calculated)
                if (clamped != calculated) {
                    taboolib.common.platform.function.debug("AttributeManager: clamped ${type.id} from $calculated to $clamped [${type.minValue}, ${type.maxValue}]")
                }
                finalValues[type.id] = clamped
                if (modsForAttr.isNotEmpty()) {
                    taboolib.common.platform.function.debug("AttributeManager: calculated $type.id: base=${type.defaultValue} + ${modsForAttr.size} modifiers = $calculated, clamped=$clamped")
                }
            }
        }

        // 4. 更新内存缓存
        attributeCache[uuid] = finalValues

        taboolib.common.platform.function.debug("AttributeManager: calculated final values for ${player.name}: ${finalValues.entries.joinToString { "${it.key}=${it.value}" }}")

        // 5. 同步到 Minecraft 原生属性系统（直接调用，调用方已保证在主线程）
        syncToVanillaInternal(player, finalValues)
    }

    private fun calculateFinal(base: Double, modifiers: List<AttributeModifier>): Double {
        var add = 0.0
        var percent = 0.0
        var multiply = 1.0

        for (mod in modifiers) {
            when (mod.type) {
                ModifierType.ADD -> add += mod.value
                ModifierType.PERCENT -> percent += mod.value
                ModifierType.MULTIPLY -> multiply *= mod.value
            }
        }

        return (base + add) * (1.0 + percent) * multiply
    }

    /**
     * 将缓存中的最终属性值同步到 Minecraft 原生属性系统
     * 用于玩家加入/重生后延迟同步，避免被 Minecraft 自身重置覆盖
     */
    fun syncToVanilla(player: Player) {
        val values = attributeCache[player.uniqueId] ?: return
        syncToVanillaInternal(player, values)
    }

    private fun syncToVanillaInternal(player: Player, values: Map<String, Double>) {
        if (!player.isOnline) return

        // 确保在主线程执行
        if (!Bukkit.isPrimaryThread()) {
            submit(async = false) {
                syncToVanillaInternal(player, values)
            }
            return
        }

        val uuid = player.uniqueId

        taboolib.common.platform.function.debug("AttributeManager: syncing vanilla attributes for ${player.name} (${values.size} calculated values)")

        // 获取上次同步的值
        val lastValues = lastSyncedValues.getOrDefault(uuid, emptyMap())
        val newValues = mutableMapOf<String, Double>()

        // 遍历所有有 vanillaName 的已注册属性
        for (type in attributeRegistry.values) {
            val vanillaName = type.vanillaName ?: continue
            val attributeId = type.id

            // 目标值：优先使用计算值，否则使用默认值
            val targetValue = values[attributeId] ?: type.defaultValue

            taboolib.common.platform.function.debug("AttributeManager: processing $attributeId ($vanillaName) target=$targetValue from values=${values[attributeId] != null}, default=${type.defaultValue}")

            // 获取 Bukkit Attribute 实例（带缓存）
            val bukkitAttribute = getVanillaAttribute(vanillaName)
            if (bukkitAttribute == null) {
                taboolib.common.platform.function.debug("AttributeManager: cannot sync $attributeId ($vanillaName) - bukkit attribute not found")
                continue
            }

            // 获取玩家的属性实例
            val instance = player.getAttribute(bukkitAttribute)
            if (instance == null) {
                taboolib.common.platform.function.debug("AttributeManager: cannot sync $attributeId ($vanillaName) - player attribute instance not found")
                continue
            }

            // 获取上次同步的值（如果有）
            val lastValue = lastValues[attributeId]

            // 如果值没有变化，跳过（增量更新）
            if (lastValue != null && lastValue == targetValue) {
                newValues[attributeId] = targetValue
                continue
            }

            // 设置基础值（直接覆盖）
            val oldBaseValue = instance.baseValue
            instance.baseValue = targetValue
            val newBaseValue = instance.baseValue

            // 记录新值
            newValues[attributeId] = targetValue

            // 调试日志
            if (lastValue != null) {
                taboolib.common.platform.function.debug("AttributeManager: updated $attributeId ($vanillaName) from $lastValue to $targetValue (base: $oldBaseValue -> $newBaseValue) for ${player.name}")
            } else {
                taboolib.common.platform.function.debug("AttributeManager: set $attributeId ($vanillaName) to $targetValue (base: $oldBaseValue -> $newBaseValue) for ${player.name}")
            }
        }

        // 更新上次同步的值缓存：始终用所有有 vanillaName 的属性的当前值更新缓存
        lastSyncedValues[uuid] = attributeRegistry.values
            .filter { it.vanillaName != null }
            .associate { type ->
                val currentValue = newValues[type.id] ?: values[type.id] ?: type.defaultValue
                type.id to currentValue
            }
    }

    /**
     * 通过 vanillaName 获取 Bukkit Attribute 实例（带缓存）
     * 主要尝试直接使用 vanillaName，如果失败则尝试添加 generic. 前缀作为备选
     */
    private fun getVanillaAttribute(vanillaName: String): org.bukkit.attribute.Attribute? {
        return vanillaAttributeCache.getOrPut(vanillaName) {
            try {
                // 首先尝试直接使用 vanillaName
                val key = NamespacedKey.minecraft(vanillaName)
                var attr = Registry.ATTRIBUTE.get(key)

                if (attr == null && !vanillaName.contains('.')) {
                    // 如果直接查找失败且名称不包含点号，尝试添加 generic. 前缀
                    val genericKey = NamespacedKey.minecraft("generic.$vanillaName")
                    attr = Registry.ATTRIBUTE.get(genericKey)
                    if (attr != null) {
                        taboolib.common.platform.function.debug("AttributeManager: found vanilla attribute 'generic.$vanillaName' (fallback)")
                    }
                }

                if (attr == null) {
                    warningS("AttributeManager: vanilla attribute '$vanillaName' not found in registry (tried '$vanillaName' and 'generic.$vanillaName')")
                } else {
                    taboolib.common.platform.function.debug("AttributeManager: found vanilla attribute '$vanillaName'")
                }
                attr
            } catch (e: Exception) {
                warningS("AttributeManager: failed to get vanilla attribute '$vanillaName': ${e.message}")
                null
            }
        }
    }

    /**
     * 为玩家添加持久化修饰符
     */
    fun addModifier(uuid: UUID, modifier: AttributeModifier) {
        taboolib.common.platform.function.debug("AttributeManager: adding modifier for $uuid: ${modifier.attributeId}=${modifier.value} ${modifier.type}")
        val data = getPlayerPersistentData(uuid)
        val mods = data.modifiers.toMutableList()
        mods.add(modifier)
        data.modifiers = mods
        savePlayerData(data)

        val player = Bukkit.getPlayer(uuid)
        if (player != null) {
            taboolib.common.platform.function.debug("AttributeManager: player online, calling doUpdate for ${player.name}")
            doUpdate(player)
        } else {
            taboolib.common.platform.function.debug("AttributeManager: player offline, update will happen on next login")
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun getEquipmentModifiers(player: Player): List<AttributeModifier> {
        // 留待后续对接
        return emptyList()
    }

    fun getAttributeValue(player: Player, attributeId: String): Double {
        val cache = attributeCache[player.uniqueId]
        if (cache != null) {
            val type = attributeRegistry[attributeId]
            return cache[attributeId] ?: type?.clamp(type.defaultValue) ?: 0.0
        }
        val type = attributeRegistry[attributeId]
        return type?.clamp(type.defaultValue) ?: 0.0
    }

    fun clearCache(uuid: UUID) {
        attributeCache.remove(uuid)
        lastSyncedValues.remove(uuid)
    }
}

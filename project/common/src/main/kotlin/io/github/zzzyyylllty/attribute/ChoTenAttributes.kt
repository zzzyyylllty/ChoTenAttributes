package io.github.zzzyyylllty.attribute

import io.github.zzzyyylllty.attribute.api.ChoTenAttributeAPI
import io.github.zzzyyylllty.attribute.api.ChoTenAttributeAPIImpl
import io.github.zzzyyylllty.attribute.logger.infoSSync
import io.github.zzzyyylllty.attribute.logger.severeS
import io.github.zzzyyylllty.attribute.logger.severeSSync
import io.github.zzzyyylllty.attribute.event.ChoTenAttributeReloadEvent
import io.github.zzzyyylllty.attribute.manager.AttributeManager
import io.github.zzzyyylllty.attribute.util.DependencyHelper
import io.github.zzzyyylllty.attribute.util.devLog
import org.bukkit.command.CommandSender
import taboolib.common.LifeCycle
import taboolib.common.env.RuntimeEnv
import taboolib.common.platform.Awake
import taboolib.common.platform.Plugin
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.console
import taboolib.common.platform.function.submit
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.module.lang.event.PlayerSelectLocaleEvent
import taboolib.module.lang.event.SystemSelectLocaleEvent


object ChoTenAttributes : Plugin() {


    @Config("config.yml")
    lateinit var config: Configuration

    val _api: ChoTenAttributeAPI? by lazy { ChoTenAttributeAPIImpl() }
    val console by lazy { console() }
    val consoleSender by lazy { console.castSafely<CommandSender>()!! }
//    val host by lazy { config.getHost("database") }
//    val dataSource by lazy { host.createDataSource() }
//    val playerDataMap = mutableMapOf<String, PlayerData>()
//    val lootMap = ConcurrentHashMap<LootLocation, LootInstance>()
    val allowedWorlds = mutableListOf<Regex>()
    var reloadTimes: Int = 0
    var enabled = false
    var allowAsyncLog = true

    var devMode = false

    fun api(): ChoTenAttributeAPI = _api!!

    @SubscribeEvent
    fun lang(event: PlayerSelectLocaleEvent) {
        event.locale = config.getString("lang", "en_US")!!
    }

    @SubscribeEvent
    fun lang(event: SystemSelectLocaleEvent) {
        event.locale = config.getString("lang", "en_US")!!
    }

    fun reloadCustomConfig(async: Boolean = true) {
        submit(async) {

            reloadTimes++
            config.reload()
            devMode = config.getBoolean("debug",false)
            AttributeManager.reloadAttributes()
            ChoTenAttributeReloadEvent().call()
        }
    }

    @Awake(LifeCycle.INIT)
    fun initDependenciesInit() {
        solveDependencies(dependencies)
        AttributeManager.registerBuiltinAttributes()
    }

    override fun onEnable() {
        enabled = true
        AttributeManager.loadAttributeFiles()
        AttributeManager.loadBasicAttributes()
        AttributeManager.startRegenerationTask()
        registerPlaceholderExpansion()
    }

    override fun onDisable() {

        enabled = false
    }

    private fun registerPlaceholderExpansion() {
        if (!DependencyHelper.papi) {
            return
        }
        try {
            val expansionClass = Class.forName("io.github.zzzyyylllty.attribute.placeholder.KAngelAttributesExpansion")
            val expansion = expansionClass.getDeclaredConstructor().newInstance()
            val registerMethod = expansionClass.getMethod("register")
            registerMethod.invoke(expansion)
            infoSSync("PlaceholderAPI expansion registered successfully.")
        } catch (e: Exception) {
            severeSSync("Failed to register PlaceholderAPI expansion: ${e.message}")
            e.printStackTrace()
        }
    }

    val dependencies = listOf(
        "adventure",
        // "arim",
        "asm",
        "caffeine",
        "datafixerupper",
        "fluxon",
        // "graaljs",
        "gson",
        "jackson",
        // "kotlincrypto",
//    "uniitem"
    )

    fun solveDependencies(dependencies: List<String>) {
        devLog("Starting loading dependencies...")
        for (name in dependencies) {
            try {
                infoSSync("Trying to load dependencies from file $name")
                val resource = ChoTenAttributes::class.java.classLoader.getResource("META-INF/dependencies/$name.json")
                if (resource == null) {
                    severeS("Resource META-INF/dependencies/$name.json not found!")
                    continue // 跳过这个依赖文件
                }

                RuntimeEnv.ENV_DEPENDENCY.loadFromLocalFile(resource)

                infoSSync("Trying to load dependencies from file $name ... DONE.")
            } catch (e: Exception) {
                severeSSync("Trying to load dependencies from file $name FAILED.")
                severeSSync("Exception: $e")
                e.printStackTrace()
            }
        }
    }

}

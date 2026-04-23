package io.github.zzzyyylllty.attribute.util

import org.bukkit.Bukkit


object DependencyHelper {

    val wg by lazy {
        isPluginInstalled("WorldGuard")
    }

    val papi by lazy {
        isPluginInstalled("PlaceholderAPI")
    }

    val attribute by lazy {
        isPluginInstalled("attribute")
    }

    val ce by lazy {
        isPluginInstalled("CraftEngine")
    }




    fun isPluginInstalled(name: String): Boolean {
        return (Bukkit.getPluginManager().getPlugin(name) != null)
    }

    fun isPremium() {

    }
}



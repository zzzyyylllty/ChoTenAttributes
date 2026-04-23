package io.github.zzzyyylllty.attribute.function

import io.github.zzzyyylllty.attribute.ChoTenAttributes.console
import io.github.zzzyyylllty.attribute.ChoTenAttributes.consoleSender
import io.github.zzzyyylllty.attribute.logger.sendStringAsComponent
import io.github.zzzyyylllty.attribute.util.VersionHelper
import org.bukkit.command.CommandSender
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.pluginVersion
import taboolib.common.platform.function.runningPlatform
import taboolib.module.lang.asLangText
import taboolib.module.nms.MinecraftVersion.versionId
import taboolib.platform.util.asLangText
import kotlin.collections.joinToString

@Awake(LifeCycle.ENABLE)
fun launchText() {

    val premiumDisplayName = if (VersionHelper().isLi2CO3Premium) {
        "<gradient:yellow:gold>" + console.asLangText("PremiumVersion")
    } else {
        "<gradient:green:aqua>" + console.asLangText("FreeVersion")
    }

    val specialThanks =
        listOf("MAORI", "NK_XingChen", "Jesuzi", "Blue_ruins(BlueIce)", "TheAchu", "CedricHunsen")

    consoleSender.sendStringAsComponent(" ")
    consoleSender.sendStringAsComponent("""<gradient:white:red>  _______      ______         ___ _________ """)
    consoleSender.sendStringAsComponent("""<gradient:white:red> / ___/ /  ___/_  __/__ ___  / _ /_  __/ _ )""")
    consoleSender.sendStringAsComponent("""<gradient:white:red>/ /__/ _ \/ _ \/ / / -_) _ \/ __ |/ / / _  |""")
    consoleSender.sendStringAsComponent("""<gradient:white:red>\___/_//_/\___/_/  \__/_//_/_/ |_/_/ /____/ """)
    consoleSender.sendStringAsComponent(" ")
    consoleSender.sendStringWithPrefix("<dark_aqua>",consoleSender.asLangText("WelcomeSeries"))
    consoleSender.sendStringWithPrefix("<dark_aqua>",consoleSender.asLangText("DesignBy", "<#ff66cc>AkaCandyKAngel</#ff66cc>"))
    consoleSender.sendStringWithPrefix("<dark_aqua>",consoleSender.asLangText("SpecialThanks","<aqua>[<dark_aqua>${specialThanks.joinToString("<dark_gray>, </dark_gray>")}<aqua>]"))
    consoleSender.sendStringWithPrefix("<dark_aqua>",consoleSender.asLangText("PoweredBy", "<#66ccff>TabooLib <gold>6.2"))
    consoleSender.sendStringAsComponent(" ")
    consoleSender.sendStringWithPrefix("<#88ccff>", console.asLangText("Welcome1"))
    consoleSender.sendStringWithPrefix("<#88ccff>", console.asLangText("Welcome2", premiumDisplayName, "${pluginVersion}<reset>", "${runningPlatform.name} - ${versionId}"))
    consoleSender.sendStringAsComponent(" ")
    consoleSender.sendStringWithPrefix("<#66bbff>", console.asLangText("Welcome3", "https://github.com/zzzyyylllty"))
    consoleSender.sendStringWithPrefix("<#66bbff>", console.asLangText("Welcome4", "https://github.com/zzzyyylllty/Lithium-Carbonate"))
    consoleSender.sendStringWithPrefix("<#66bbff>", console.asLangText("Welcome5", "https://chotengroup.gitbook.io/attribute"))
    consoleSender.sendStringAsComponent(" ")
    if (VersionHelper().isLi2CO3Premium) consoleSender.sendStringWithPrefix("<gradient:red:yellow:green:aqua:light_purple>", console.asLangText("PremiumVersionWelcome", premiumDisplayName))
    consoleSender.sendStringAsComponent(" ")

}

private fun CommandSender.sendStringWithPrefix(prefix: String, message: String) {
    this.sendStringAsComponent(prefix + message)
}
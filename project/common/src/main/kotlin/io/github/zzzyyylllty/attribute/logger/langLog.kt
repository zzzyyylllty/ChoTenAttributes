package io.github.zzzyyylllty.attribute.logger

import io.github.zzzyyylllty.attribute.ChoTenAttributes
import io.github.zzzyyylllty.attribute.ChoTenAttributes.console
import io.github.zzzyyylllty.attribute.ChoTenAttributes.consoleSender
import io.github.zzzyyylllty.attribute.ChoTenAttributes.allowAsyncLog
import io.github.zzzyyylllty.attribute.ChoTenAttributes.console
import io.github.zzzyyylllty.attribute.ChoTenAttributes.consoleSender
import io.github.zzzyyylllty.attribute.logger.infoS
import io.github.zzzyyylllty.attribute.logger.severeS
import io.github.zzzyyylllty.attribute.logger.warningS
import io.github.zzzyyylllty.attribute.util.minimessage.legacyToMiniMessage
import io.github.zzzyyylllty.attribute.util.mmUtil
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.submit
import taboolib.module.lang.asLangText

val prefix = "[<gradient:#66ffff:#99ccff:#aa99cc>attribute</gradient>]"


fun infoL(node: String,vararg args: Any) {
    infoS(console.asLangText(node,*args))
}
fun infoLSync(node: String,vararg args: Any) {
    infoSSync(console.asLangText(node,*args))
}
fun severeL(node: String,vararg args: Any) {
    severeS(console.asLangText(node,*args))
}
fun warningL(node: String,vararg args: Any) {
    warningS(console.asLangText(node,*args))
}


fun fineS(message: String) {
    submit(async = allowAsyncLog) {
        consoleSender.sendStringAsComponent("<gray>$prefix [<#66ffcc>FINE</#66ffcc>]</gray> <reset>$message")
    }
}

fun debugS(message: String) {
    if (ChoTenAttributes.enabled && allowAsyncLog) {
        consoleSender.sendStringAsComponent("<gray>$prefix [<#ddaa77>DEBUG</#ddaa77>]</gray> <#aaaaaa>$message")
    } else consoleSender.sendStringAsComponent("<gray>$prefix [<#ddaa77>DEBUG</#ddaa77>]</gray> <#aaaaaa>$message")
}

fun infoS(message: String) {
    submit(async = allowAsyncLog) {
        consoleSender.sendStringAsComponent("<gray>$prefix [<#66ccff>INFO</#66ccff>]</gray> <reset>$message")
    }
}

fun warningS(message: String) {
    submit(async = allowAsyncLog) {
        consoleSender.sendStringAsComponent("<gray>$prefix [<#ffee66>WARN</#ffee66>]</gray> <#eeeeaa>$message")
    }
}

fun severeS(message: String) {
    submit(async = allowAsyncLog) {
        consoleSender.sendStringAsComponent("<gray>$prefix [<#ff6600>ERROR</#ff6600>]</gray> <#ffccbb>$message")
    }
}
fun debugSSync(message: String) {
    submit(async = allowAsyncLog) {
        consoleSender.sendStringAsComponent("<gray>$prefix [<#ddaa77>DEBUG</#ddaa77>]</gray> <#aaaaaa>$message")
    }
}

fun infoSSync(message: String) {
    submit(async = allowAsyncLog) {
        consoleSender.sendStringAsComponent("<gray>$prefix [<#66ccff>INFO</#66ccff>]</gray> <reset>$message")
    }
}

fun warningSSync(message: String) {
    submit(async = allowAsyncLog) {
        consoleSender.sendStringAsComponent("<gray>$prefix [<#ffee66>WARN</#ffee66>]</gray> <#eeeeaa>$message")
    }
}

fun severeSSync(message: String) {
    submit(async = allowAsyncLog) {
        consoleSender.sendStringAsComponent("<gray>$prefix [<#ff6600>ERROR</#ff6600>]</gray> <#ffccbb>$message")
    }
}

fun CommandSender.sendStringAsComponent(message: String) {
    val sender = this
        (sender as Audience).sendMessage(mmUtil.deserialize(message.legacyToMiniMessage()))
}

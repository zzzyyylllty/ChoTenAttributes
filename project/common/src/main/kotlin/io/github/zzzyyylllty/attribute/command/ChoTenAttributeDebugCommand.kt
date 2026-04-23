package io.github.zzzyyylllty.attribute.command

import io.github.zzzyyylllty.attribute.logger.sendStringAsComponent
import io.github.zzzyyylllty.attribute.api.ChoTenAttributeAPI
import org.bukkit.entity.Player
import taboolib.common.platform.command.*
import taboolib.platform.util.asLangText

@CommandHeader(
    name = "chotenattribute-debug",
    aliases = ["attributedebug", "atbdebug"],
    permission = "chotenattribute.command.debug",
    description = "Debug Command for ChoTenAttribute.",
    permissionDefault = PermissionDefault.OP,
    newParser = false
)
object ChoTenAttributeDebugCommand {

    @CommandBody
    val main = mainCommand {
        createModernHelper()
    }

    @CommandBody
    val show = subCommand {
        execute<Player> { player, _, _ ->
            val all = ChoTenAttributeAPI.instance.getAllAttributes(player)
            player.sendStringAsComponent(player.asLangText("Command_Debug_Show_Header"))
            all.forEach { (id, value) ->
                player.sendStringAsComponent(player.asLangText("Command_Debug_Show_Item", id, value))
            }
        }
    }
}

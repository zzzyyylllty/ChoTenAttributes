package io.github.zzzyyylllty.attribute.command

import io.github.zzzyyylllty.attribute.api.ChoTenAttributeAPI
import io.github.zzzyyylllty.attribute.data.AttributeModifier
import io.github.zzzyyylllty.attribute.data.ModifierSource
import io.github.zzzyyylllty.attribute.data.ModifierType
import io.github.zzzyyylllty.attribute.manager.AttributeManager
import io.github.zzzyyylllty.attribute.logger.sendStringAsComponent
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import taboolib.common.platform.command.*
import taboolib.platform.util.asLangText
import java.util.*

@CommandHeader(
    name = "chotenattribute-manage",
    aliases = ["attributemanage", "atbmanage"],
    permission = "chotenattribute.command.manage",
    description = "manage Command for ChoTenAttribute.",
    permissionDefault = PermissionDefault.OP,
    newParser = false
)
object ChoTenAttributeManageCommand {

    @CommandBody
    val main = mainCommand {
        createModernHelper()
    }

    @CommandBody
    val add = subCommand {
        dynamic("player") {
            suggestion<CommandSender> { _, _ -> Bukkit.getOnlinePlayers().map { it.name } }
            dynamic("attrId") {
                suggestion<CommandSender> { _, _ -> AttributeManager.attributeRegistry.keys.toList() }
                dynamic("value") {
                    dynamic("type") {
                        suggestion<CommandSender> { _, _ -> ModifierType.entries.map { it.name } }
                        execute<CommandSender> { sender, context, _ ->
                            val player = Bukkit.getPlayer(context["player"]) ?: return@execute sender.sendStringAsComponent(sender.asLangText("Command_Error_Player_Not_Found", context["player"]))
                            val attrId = context["attrId"]
                            val value = context["value"].toDoubleOrNull() ?: return@execute sender.sendStringAsComponent(sender.asLangText("Command_Error_Invalid_Value", context["value"]))
                            val type = runCatching { ModifierType.valueOf(context["type"].uppercase()) }.getOrNull() ?: return@execute sender.sendStringAsComponent(sender.asLangText("Command_Error_Invalid_Type", context["type"]))

                            val modifier = AttributeModifier(
                                attributeId = attrId,
                                value = value,
                                type = type,
                                source = ModifierSource.OTHER
                            )

                            ChoTenAttributeAPI.instance.addModifier(player.uniqueId, modifier)
                            sender.sendStringAsComponent(sender.asLangText("Command_Manage_Add_Success", attrId, value, type.name, player.name))
                        }
                    }
                }
            }
        }
    }

    @CommandBody
    val set = subCommand {
        dynamic("player") {
            suggestion<CommandSender> { _, _ -> Bukkit.getOnlinePlayers().map { it.name } }
            dynamic("attrId") {
                suggestion<CommandSender> { _, _ -> AttributeManager.attributeRegistry.keys.toList() }
                dynamic("value") {
                    dynamic("type") {
                        suggestion<CommandSender> { _, _ -> ModifierType.entries.map { it.name } }
                        execute<CommandSender> { sender, context, _ ->
                            val player = Bukkit.getOfflinePlayer(context["player"])
                            val attrId = context["attrId"]
                            val value = context["value"].toDoubleOrNull() ?: return@execute sender.sendStringAsComponent(sender.asLangText("Command_Error_Invalid_Value", context["value"]))
                            val type = runCatching { ModifierType.valueOf(context["type"].uppercase()) }.getOrNull() ?: return@execute sender.sendStringAsComponent(sender.asLangText("Command_Error_Invalid_Type", context["type"]))

                            val data = AttributeManager.getPlayerPersistentData(player.uniqueId)
                            val mods = data.modifiers.toMutableList()
                            mods.removeIf { it.attributeId == attrId && it.source == ModifierSource.OTHER }

                            val modifier = AttributeModifier(
                                attributeId = attrId,
                                value = value,
                                type = type,
                                source = ModifierSource.OTHER
                            )
                            mods.add(modifier)

                            data.modifiers = mods
                            AttributeManager.savePlayerData(data)

                            Bukkit.getPlayer(player.uniqueId)?.let { AttributeManager.updatePlayer(it) }
                            sender.sendStringAsComponent(sender.asLangText("Command_Manage_Set_Success", attrId, value, type.name, player.name ?: "Unknown"))
                        }
                    }
                }
            }
        }
    }

    @CommandBody
    val remove = subCommand {
        dynamic("player") {
            suggestion<CommandSender> { _, _ -> Bukkit.getOnlinePlayers().map { it.name } }
            dynamic("uuid") {
                suggestion<CommandSender> { sender, context -> AttributeManager.getPlayerPersistentData(Bukkit.getOfflinePlayer(context["player"]).uniqueId).modifiers.map { it.uuid.toString() } }
                execute<CommandSender> { sender, context, _ ->
                    val player = Bukkit.getOfflinePlayer(context["player"])
                    val uuidString = context["uuid"]
                    val uuid = runCatching { UUID.fromString(uuidString) }.getOrNull() ?: return@execute sender.sendMessage("§cInvalid UUID.")

                    ChoTenAttributeAPI.instance.removeModifier(player.uniqueId, uuid)
                    sender.sendStringAsComponent(sender.asLangText("Command_Manage_Remove_Attempt", uuidString, player.name ?: "Unknown"))
                }
            }
        }
    }

    @CommandBody
    val update = subCommand {
        dynamic("player") {
            suggestion<CommandSender> { _, _ -> Bukkit.getOnlinePlayers().map { it.name } }
            execute<CommandSender> { sender, context, _ ->
                val player = Bukkit.getPlayer(context["player"]) ?: return@execute sender.sendStringAsComponent(sender.asLangText("Command_Error_Player_Not_Found", context["player"]))
                ChoTenAttributeAPI.instance.updatePlayerAttributes(player)
                sender.sendStringAsComponent(sender.asLangText("Command_Manage_Update_Success", player.name))
            }
        }
    }

    @CommandBody
    val list = subCommand {
        dynamic("player") {
            suggestion<CommandSender> { _, _ -> Bukkit.getOnlinePlayers().map { it.name } }
            execute<CommandSender> { sender, context, _ ->
                val player = Bukkit.getOfflinePlayer(context["player"])
                val data = AttributeManager.getPlayerPersistentData(player.uniqueId)

                sender.sendStringAsComponent(sender.asLangText("Command_Manage_List_Header", player.name ?: "Unknown"))
                if (data.modifiers.isEmpty()) {
                    sender.sendStringAsComponent(sender.asLangText("Command_Manage_List_Empty"))
                } else {
                    data.modifiers.filterIsInstance<AttributeModifier>().forEach {
                        sender.sendStringAsComponent(sender.asLangText("Command_Manage_List_Item", it.attributeId, it.value, it.type.name, it.uuid.toString()))
                    }
                }
            }
        }
    }

    @CommandBody
    val clear = subCommand {
        dynamic("player") {
            suggestion<CommandSender> { _, _ -> Bukkit.getOnlinePlayers().map { it.name } }
            execute<CommandSender> { sender, context, _ ->
                val player = Bukkit.getOfflinePlayer(context["player"])
                val data = AttributeManager.getPlayerPersistentData(player.uniqueId)
                data.modifiers = emptyList()
                AttributeManager.savePlayerData(data)

                Bukkit.getPlayer(player.uniqueId)?.let { AttributeManager.updatePlayer(it) }
                sender.sendStringAsComponent(sender.asLangText("Command_Manage_Clear_Success", player.name ?: "Unknown"))
            }
        }
    }
}

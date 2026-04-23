package io.github.zzzyyylllty.attribute.command

import io.github.zzzyyylllty.attribute.function.player.sendComponent
import io.github.zzzyyylllty.attribute.function.kether.runKether
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.PermissionDefault
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.player
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.function.submitAsync

@CommandHeader(
    name = "attributeapi",
    aliases = ["attributedebug", "atbapi"],
    permission = "chotenattribute.command.api",
    description = "API Command of DepazItems.",
    permissionMessage = "",
    permissionDefault = PermissionDefault.OP,
    newParser = false,
)
object ChoTenAttributeApiCommand {

    @CommandBody
    val main = mainCommand {
        createModernHelper()
    }

    @CommandBody
    val help = subCommand {
        createModernHelper()
    }

    /** 解析 Minimessage */
    @CommandBody
    val minimessage = subCommand {
        dynamic("content") {
            execute<CommandSender> { sender, context, argument ->
                submitAsync {
                    val mm = MiniMessage.miniMessage()
                    // 获取参数的值
                    val content = context["content"]
                    sender.sendComponent(content)
                }
            }
        }
    }

    /** Kether */
    @CommandBody
    val eval = subCommand {
        dynamic("script") {
            execute<CommandSender> { sender, context, argument ->
                submitAsync {
                    val mm = MiniMessage.miniMessage()
                    // 获取参数的值
                    val content = context["script"]
                    val ret = runKether(listOf(content), sender)
                    sender.sendComponent("<yellow>Kether: <gray>$content")
                    sender.sendComponent("<yellow>Return: <gray>${ret.get()}")
                }
            }
        }
    }

    /** Kether */
    @CommandBody
    val evalByPlayer = subCommand {
        player("player") {
            dynamic("script") {
                execute<CommandSender> { sender, context, argument ->
                    submitAsync {
                        val tabooPlayer = context.player("player")
                        val bukkitPlayer = tabooPlayer.castSafely<Player>() as CommandSender
                        val mm = MiniMessage.miniMessage()
                        // 获取参数的值
                        val content = context["script"]
                        val ret = runKether(listOf(content), bukkitPlayer)
                        sender.sendComponent("<yellow>Kether: <gray>$content")
                        sender.sendComponent("<yellow>Return: <gray>${ret.get()}")
                    }
                }
            }
        }
    }
    /** Kether */
    @CommandBody
    val evalSilent = subCommand {
        dynamic("script") {
            execute<CommandSender> { sender, context, argument ->
                submitAsync {
                    val mm = MiniMessage.miniMessage()
                    // 获取参数的值
                    val content = context["script"]
                    runKether(listOf(content), sender)
                }
            }
        }
    }

    /** Kether */
    @CommandBody
    val evalByPlayerSilent = subCommand {
        player("player") {
            dynamic("script") {
                execute<CommandSender> { sender, context, argument ->
                    submitAsync {
                        val tabooPlayer = context.player("player")
                        val bukkitPlayer = tabooPlayer.castSafely<Player>() as CommandSender
                        val mm = MiniMessage.miniMessage()
                        // 获取参数的值
                        val content = context["script"]
                        runKether(listOf(content), bukkitPlayer)
                    }
                }
            }
        }
    }


}

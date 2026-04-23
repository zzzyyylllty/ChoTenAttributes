package io.github.zzzyyylllty.attribute.command

import io.github.zzzyyylllty.attribute.ChoTenAttributes.reloadCustomConfig
import io.github.zzzyyylllty.attribute.function.player.sendComponent
import io.github.zzzyyylllty.attribute.logger.fineS
import io.github.zzzyyylllty.attribute.logger.infoS
import io.github.zzzyyylllty.attribute.logger.severeS
import org.bukkit.command.CommandSender
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.PermissionDefault
import taboolib.common.platform.command.component.CommandComponent
import taboolib.common.platform.command.component.CommandComponentDynamic
import taboolib.common.platform.command.component.CommandComponentLiteral
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.function.pluginVersion
import taboolib.common.platform.function.runningPlatform
import taboolib.module.lang.asLangText
import taboolib.module.nms.MinecraftVersion.minecraftVersion

@CommandHeader(
    name = "chotenattribute",
    aliases = ["attribute","atb"],
    permission = "chotenattribute.command.main",
    description = "Main Command for ChoTenAttribute.",
    permissionMessage = "",
    permissionDefault = PermissionDefault.OP,
    newParser = false,
)
object ChoTenAttributeMainCommand {

    @CommandBody
    val about = subCommand {
        execute<CommandSender> { sender, context, argument -> //
            sender.sendComponent("<gradient:aqua:yellow>ChoTenAttribute</gradient> <#ccccff>$pluginVersion")
            sender.sendComponent("<gradient:#6600ff:#aa00aa>Running on:</gradient> <light_purple>${runningPlatform.name} - $minecraftVersion")
            sender.sendComponent("<#660099>Plugin by AkaCandyKAngel.")
        }
    }

    @CommandBody
    val reload = subCommand {
        execute<CommandSender> { sender, context, argument ->
            infoS("Reloading...")
            try {
                reloadCustomConfig()
                fineS("Reloaded.")
            } catch (e: Exception) {
                severeS("<red>Error thrown. check console for info.</red>")
                e.printStackTrace()
            }
        }
    }

    @CommandBody
    val main = mainCommand {
        createModernHelper()
    }
    @CommandBody
    val debug = ChoTenAttributeDebugCommand
    @CommandBody
    val manage = ChoTenAttributeManageCommand
//    @CommandBody
//    val Data = ChoTenDataCommand

}

fun CommandComponent.createModernHelper(checkPermissions: Boolean = true) {
    execute<ProxyCommandSender> { sender, context, _ ->
        val command = context.command
        val builder = StringBuilder("<gradient:yellow:aqua>Usage: /${command.name}<gradient>")
        var newline = false

        fun check(children: List<CommandComponent>): List<CommandComponent> {
            // 检查权限
            val filterChildren = if (checkPermissions) {
                children.filter { sender.hasPermission(it.permission) }
            } else {
                children
            }
            // 过滤隐藏
            return filterChildren.filter { it !is CommandComponentLiteral || !it.hidden }
        }

        fun space(space: Int): String {
            return (1..space).joinToString("") { " " }
        }

        fun print(compound: CommandComponent, index: Int, size: Int, offset: Int = 8, level: Int = 0, end: Boolean = false, optional: Boolean = false) {
            var option = optional
            var comment = 0
            when (compound) {
                is CommandComponentLiteral -> {
                    if (size == 1) {
                        builder.append(" ").append("<gradient:#ccaaff:#9900ff:#ff0099>${compound.aliases[0]}<>")
                    } else {
                        newline = true
                        builder.appendLine()
                        builder.append(space(offset))
                        if (level > 1) {
                            builder.append(if (end) " " else "<#888888>│")
                        }
                        builder.append(space(level))
                        if (index + 1 < size) {
                            builder.append("<gradient:#888888:#cccccc>├── </gradient>")
                        } else {
                            builder.append("<gradient:#888888:#cccccc>└── </gradient>")
                        }
                        builder.append("<gradient:#ccaaff:#9900ff:#ff0099>${compound.aliases[0]}</gradient>")
                    }
                    option = false
                    comment = compound.aliases[0].length
                }
                is CommandComponentDynamic -> {
                    val value = if (compound.comment.startsWith("@")) {
                        sender.asLangText(compound.comment.substring(1))
                    } else {
                        compound.comment
                    }
                    comment = if (compound.optional || option) {
                        option = true
                        builder.append(" ").append("<#66ffcc>[<$value>]")
                        compound.comment.length + 4
                    } else {
                        builder.append(" ").append("<#ffcc66><$value>")
                        compound.comment.length + 2
                    }
                }
            }
            if (level > 0) {
                comment += 1
            }
            val checkedChildren = check(compound.children)
            checkedChildren.forEachIndexed { i, children ->
                // 因 literal 产生新的行
                if (newline) {
                    print(children, i, checkedChildren.size, offset, level + comment, end, option)
                } else {
                    val length = if (offset == 8) command.name.length + 1 else comment + 1
                    print(children, i, checkedChildren.size, offset + length, level, end, option)
                }
            }
        }
        val checkedChildren = check(context.commandCompound.children)
        val size = checkedChildren.size
        checkedChildren.forEachIndexed { index, children ->
            print(children, index, size, end = index + 1 == size)
        }
        builder.lines().forEach {
            sender.castSafely<CommandSender>()?.sendComponent(it)
        }
    }
}

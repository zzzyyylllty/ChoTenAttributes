package io.github.zzzyyylllty.attribute.function.player

import io.github.zzzyyylllty.attribute.util.toComponent
import org.bukkit.command.CommandSender

fun CommandSender.sendComponent(message: String) {
    if (message.isNotEmpty()) sendMessage(message.toComponent())
}
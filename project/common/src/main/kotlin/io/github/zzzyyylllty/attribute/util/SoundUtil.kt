package io.github.zzzyyylllty.attribute.util

import io.github.zzzyyylllty.attribute.ChoTenAttributes.config
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.entity.Player
import taboolib.common.platform.function.submit

object SoundUtil {
    fun playSound(player: Player, soundName: String, source: Sound.Source, volume: Float, pitch: Float) {
        submit { player.playSound(Sound.sound(
            Key.key(soundName),
            source,
            volume,
            pitch
        ))
        }
    }

    fun playConfiguredSound(player: Player, soundName: String) {
        val split = config.getString("sounds.$soundName")?.split(" ") ?: return
        val sound = split[0]
        val volume = if (split.size >= 2) split[1].toFloat() else 1.0F
        val pitch = if (split.size >= 3) split[2].toFloat() else 1.0F
        val source = if (split.size >= 4) Sound.Source.valueOf(split[3].uppercase()) else Sound.Source.MASTER
        playSound(player, sound, source, volume, pitch)
    }
}
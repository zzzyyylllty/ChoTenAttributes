package io.github.zzzyyylllty.attribute.util

import io.github.zzzyyylllty.attribute.function.kether.parseKether
import org.bukkit.entity.Player
import taboolib.common.util.random

fun String?.asNumberFormat(player: Player?): Double {
    val oAmount = this ?: "1"
    val full = if (oAmount.contains("{")) oAmount.parseKether(player) else oAmount
    val split = full.split("~")
    return if (split.size >= 2) random(split.first().toDoubleOrNull() ?: 1.0, split.last().toDoubleOrNull() ?: 1.0) else full.toDoubleOrNull() ?: 1.0
}
fun String?.asNumberFormatNullable(player: Player?): Double? {
    val oAmount = this ?: "1"
    val full = if (oAmount.contains("{")) oAmount.parseKether(player) else oAmount
    val split = full.split("~")
    return if (split.size >= 2) random(split.first().toDoubleOrNull() ?: 1.0, split.last().toDoubleOrNull() ?: 1.0) else full.toDoubleOrNull()
}
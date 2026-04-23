package io.github.zzzyyylllty.attribute.util

import io.github.zzzyyylllty.attribute.ChoTenAttributes.config
import net.kyori.adventure.text.Component

fun String.toComponent(): Component {
    return mmUtil.deserialize(this)
}
fun String.toComponentJson(): String {
    return mmJsonUtil.serialize(this.toComponent())
}

fun List<String>.toComponent(): List<Component> {
    return if (config.getBoolean("performance.adventure.use-split-replace-list-serialize", false)) listOf(mmUtil.deserialize(this.joinToString("<br>"))) else this.map { mmUtil.deserialize(it) }
}

fun Any?.serializeComponent(): Any? {
    return when (val input = this) {
        is Map<*, *> -> input.map { (k, v) ->
            k.toString() to v.serializeComponent()
        }.toMap() // 直接使用 map 和 toMap

        is List<*> -> input.map { it.serializeComponent() } // 使用 map

        is String -> input.toComponent()

        else -> input
    }
}
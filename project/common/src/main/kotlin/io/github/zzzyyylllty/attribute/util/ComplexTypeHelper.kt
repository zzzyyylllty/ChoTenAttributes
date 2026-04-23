package io.github.zzzyyylllty.attribute.util

import kotlin.text.lowercase
import kotlin.text.toBooleanStrictOrNull

fun Any.toBooleanTolerance(): Boolean {
    return when (this) {
        is Boolean -> this
        is Int -> this > 0
        is String -> this.lowercase() == "true" || this == "1"
        is Double -> this > 0.0
        is Float -> this > 0.0
        is Byte -> (this == 1.toByte())
        is Short -> this > 0
        is Long -> this > 0
        else -> this.toString().toBooleanStrictOrNull() ?: false
    }
}
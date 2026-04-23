package io.github.zzzyyylllty.attribute.util

import io.github.zzzyyylllty.attribute.ChoTenAttributes
import io.github.zzzyyylllty.attribute.ChoTenAttributes.consoleSender
import io.github.zzzyyylllty.attribute.logger.debugS

fun devLog(input: String) {
    if (ChoTenAttributes.devMode) debugS(input)
}

fun devMode(b: Boolean) {
    ChoTenAttributes.devMode = b
}
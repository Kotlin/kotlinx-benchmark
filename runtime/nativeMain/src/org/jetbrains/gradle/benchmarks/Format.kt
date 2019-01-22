package org.jetbrains.gradle.benchmarks

import kotlinx.cinterop.*
import platform.posix.*

actual fun Double.format(precision: Int): String = memScoped {
    val bytes = allocArray<ByteVar>(100)
    sprintf(bytes, "%.${precision}F", this@format)
    val text = bytes.toKString()
    return text.replace(Regex("\\B(?=(\\d{3})+(?!\\d))"), ",")
}
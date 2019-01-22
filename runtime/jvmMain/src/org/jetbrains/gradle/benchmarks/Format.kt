package org.jetbrains.gradle.benchmarks

actual fun Double.format(precision: Int): String {
    val text = "%.${precision}f".format(this)
    return text.replace(Regex("\\B(?=(\\d{3})+(?!\\d))"), ",")
}
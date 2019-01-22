package org.jetbrains.gradle.benchmarks

actual fun Double.format(precision: Int): String {
    val text = this.asDynamic().toFixed(precision) as String
    return text.replace(Regex("\\B(?=(\\d{3})+(?!\\d))"), ",")
}
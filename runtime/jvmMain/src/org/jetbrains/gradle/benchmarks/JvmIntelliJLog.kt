package org.jetbrains.gradle.benchmarks

actual fun String.toByteArrayUtf8(): ByteArray = toByteArray(Charsets.UTF_8)
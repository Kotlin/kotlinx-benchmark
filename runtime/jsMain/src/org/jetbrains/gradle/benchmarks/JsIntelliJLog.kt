package org.jetbrains.gradle.benchmarks

// TODO: Use UTF8 from kotlinx.io, it's okay while we don't have any non-ascii chars
actual fun String.toByteArrayUtf8(): ByteArray = map { it.toByte() }.toByteArray()

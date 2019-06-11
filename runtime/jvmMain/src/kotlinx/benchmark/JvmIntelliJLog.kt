package kotlinx.benchmark

actual fun String.toByteArrayUtf8(): ByteArray = toByteArray(Charsets.UTF_8)
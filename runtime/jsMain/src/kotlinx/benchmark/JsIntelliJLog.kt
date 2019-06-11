package kotlinx.benchmark

// TODO: Use UTF8 from kotlinx.io, it's okay while we don't have any non-ascii chars
@Suppress("UNUSED_VARIABLE") // KT-23633
actual fun String.toByteArrayUtf8(): ByteArray {
    val s = this
    val block = js("unescape(encodeURIComponent(s))") // contains only chars that fit to byte
    return (block as String).toList().map { it.toByte() }.toByteArray()
}
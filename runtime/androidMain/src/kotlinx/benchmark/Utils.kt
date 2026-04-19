package kotlinx.benchmark


internal actual fun Double.format(precision: Int, useGrouping: Boolean): String =
    error("Not supported on Android yet")

internal actual fun String.writeFile(text: String): Unit =
    error("Not supported on Android yet")

internal actual fun String.readFile(): String =
    error("Not supported on Android yet")

internal actual inline fun measureNanoseconds(block: () -> Unit): Long =
    error("Not supported on Android yet")

package kotlinx.benchmark

import java.io.*

internal actual fun Double.format(precision: Int, useGrouping: Boolean): String {
    return if (useGrouping) "%,.0${precision}f".format(this)
    else "%.0${precision}f".format(this)
}

internal actual fun String.readFile(): String {
    return File(this).readText()
}

internal actual fun String.writeFile(text: String) {
    File(this).writeText(text)
}

internal actual inline fun measureNanoseconds(block: () -> Unit): Long = TODO("Not implemented for this platform")
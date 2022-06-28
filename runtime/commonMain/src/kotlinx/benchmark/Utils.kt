package kotlinx.benchmark

internal expect fun String.readFile(): String

internal expect fun String.writeFile(text: String)

internal expect inline fun measureNanoseconds(block: () -> Unit): Long
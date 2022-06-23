package kotlinx.benchmark

internal expect fun String.readFile(): String

internal expect fun String.writeFile(text: String)

/*
* Measure time in nanoseconds for given body
 */
internal expect inline fun measureTime(block: () -> Unit): Long
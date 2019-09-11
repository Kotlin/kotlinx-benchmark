package kotlinx.benchmark

actual class Blackhole {
    actual fun consume(obj: Any?) {}
    actual fun consume(bool: Boolean) {}
    actual fun consume(c: Char) {}
    actual fun consume(b: Byte) {}
    actual fun consume(s: Short) {}
    actual fun consume(i: Int) {}
    actual fun consume(l: Long) {}
    actual fun consume(f: Float) {}
    actual fun consume(d: Double) {}
}

package kotlinx.benchmark

import kotlinx.cinterop.convert
import kotlinx.cinterop.toByte
import kotlinx.benchmark.impl.*
import kotlin.native.identityHashCode

actual class Blackhole {
    actual fun consume(obj: Any?) {
        val hc = obj?.identityHashCode() ?: 0
        consumeImplULL(hc.convert())
    }
    actual fun consume(bool: Boolean) {
        consumeImplULL(bool.toByte().convert())
    }
    actual fun consume(c: Char) {
        consumeImplULL(c.code.convert())
    }
    actual fun consume(b: Byte) {
        consumeImplULL(b.convert())
    }
    actual fun consume(s: Short) {
        consumeImplULL(s.convert())
    }
    actual fun consume(i: Int) {
        consumeImplULL(i.convert())
    }
    actual fun consume(l: Long) {
        consumeImplULL(l.convert())
    }
    actual fun consume(f: Float) {
        consumeImplF(f)
    }
    actual fun consume(d: Double) {
        consumeImplD(d)
    }
}

actual fun Blackhole.flush() = Unit

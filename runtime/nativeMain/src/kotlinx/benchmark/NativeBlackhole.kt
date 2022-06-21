package kotlinx.benchmark

import kotlinx.cinterop.pin

actual class Blackhole {
    actual fun consume(obj: Any?) {
        obj?.pin()
    }
    actual fun consume(bool: Boolean) {
        bool.pin()
    }
    actual fun consume(c: Char) {
        c.pin()
    }
    actual fun consume(b: Byte) {
        b.pin()
    }
    actual fun consume(s: Short) {
        s.pin()
    }
    actual fun consume(i: Int) {
        i.pin()
    }
    actual fun consume(l: Long) {
        l.pin()
    }
    actual fun consume(f: Float) {
        f.pin()
    }
    actual fun consume(d: Double) {
        d.pin()
    }
}

actual fun Blackhole.flush() = Unit
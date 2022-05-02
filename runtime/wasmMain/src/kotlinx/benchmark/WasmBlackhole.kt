package kotlinx.benchmark

actual class Blackhole {
    actual fun consume(obj: Any?) {
        obj?.hashCode()
    }

    actual fun consume(bool: Boolean) {
        bool.hashCode()
    }

    actual fun consume(c: Char) {
        c.hashCode()
    }

    actual fun consume(b: Byte) {
        b.hashCode()
    }

    actual fun consume(s: Short) {
        s.hashCode()
    }

    actual fun consume(i: Int) {
        i.hashCode()
    }

    actual fun consume(l: Long) {
        l.hashCode()
    }

    actual fun consume(f: Float) {
        f.hashCode()
    }

    actual fun consume(d: Double) {
        d.hashCode()
    }
}
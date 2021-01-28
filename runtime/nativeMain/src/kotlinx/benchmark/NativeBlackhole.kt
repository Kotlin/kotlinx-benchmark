package kotlinx.benchmark

actual class Blackhole {
    @ThreadLocal
    companion object {
        var consumer = 0
    }
    actual fun consume(obj: Any?) {
        // hashCode now is implemented as taking address of object, so it's suitable now.
        // If implementation is changed `Blackhole` should be reimplemented.
        consumer += obj.hashCode()
    }
    actual fun consume(bool: Boolean) {
        consumer += bool.hashCode()
    }
    actual fun consume(c: Char) {
        consumer += c.hashCode()
    }
    actual fun consume(b: Byte) {
        consumer += b.hashCode()
    }
    actual fun consume(s: Short) {
        consumer += s.hashCode()
    }
    actual fun consume(i: Int) {
        consumer += i.hashCode()
    }
    actual fun consume(l: Long) {
        consumer += l.hashCode()
    }
    actual fun consume(f: Float) {
        consumer += f.hashCode()
    }
    actual fun consume(d: Double) {
        consumer += d.hashCode()
    }
}

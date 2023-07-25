package kotlinx.benchmark

import kotlinx.cinterop.convert
import kotlinx.cinterop.toByte
import kotlin.concurrent.Volatile
import kotlin.native.ref.WeakReference
import kotlin.random.Random
import kotlinx.benchmark.impl.*

@OptIn(ExperimentalStdlibApi::class)
actual class Blackhole {
    /*
     * For primitive types, DCE may be omitted by creating an artificial use via inline asm.
     * For objects, there is no cheap way to obtain something that could be passed to a native function with such
     * inline asm, so instead we're generating a sequence of random numbers and as soon as we get zero (that's highly
     * unlikely to happen) an object will be wrapped into a weak reference and stored into a field.
     */
    @Volatile
    private var targetLcgValue: Int = Random.nextInt()
    private var lcg: Int = Random.nextInt()
    @Volatile
    internal var ref: WeakReference<Any>? = null

    actual fun consume(obj: Any?) {
        // https://en.wikipedia.org/wiki/Linear_congruential_generator
        val next = lcg * 1664525 + 1013904223
        if ((next and targetLcgValue) == 0) {
            if (obj !== null) {
                ref = WeakReference(obj)
            }
            targetLcgValue *= 17
        }
        lcg = next
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

actual fun Blackhole.flush()  {
    ref?.clear()
}

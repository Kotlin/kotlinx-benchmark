package kotlinx.benchmark

import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi
import kotlin.random.Random

/**
 * Best-effort blackhole for the Android ART runtime.
 * Uses the same comparison-based technique as `NativeBlackhole`
 */
@Suppress("NOTHING_TO_INLINE")
actual class Blackhole {
    @Volatile var i0: Int = Random.nextInt()
    var i1 = i0 + 1

    @Volatile var l0: Long = Random.nextLong()
    var l1 = l0 + 1L

    @Volatile var bh: Blackhole? = null

    actual inline fun consume(obj: Any?) = consume(System.identityHashCode(obj))
    actual inline fun consume(bool: Boolean) = consume(if (bool) 1 else 0)
    actual inline fun consume(c: Char) = consume(c.code)
    actual inline fun consume(b: Byte) = consume(b.toInt())
    actual inline fun consume(s: Short) = consume(s.toInt())

    actual inline fun consume(i: Int) {
        if ((i0 == i) && (i1 == i)) {
            bh!!.i0 = i
        }
    }

    actual inline fun consume(l: Long) {
        if ((l0 == l) && (l1 == l)) {
            bh!!.l0 = l
        }
    }

    actual inline fun consume(f: Float) = consume(f.toBits())
    actual inline fun consume(d: Double) = consume(d.toBits())
}

@KotlinxBenchmarkRuntimeInternalApi
actual fun Blackhole.flush() = Unit

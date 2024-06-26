package kotlinx.benchmark

import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi
import kotlinx.cinterop.toByte
import kotlin.concurrent.Volatile
import kotlin.native.identityHashCode
import kotlin.random.Random

@OptIn(ExperimentalStdlibApi::class)
actual class Blackhole {
    @KotlinxBenchmarkRuntimeInternalApi
    @Volatile
    var i0: Int = Random.nextInt()
    @KotlinxBenchmarkRuntimeInternalApi
    var i1 = i0 + 1

    @KotlinxBenchmarkRuntimeInternalApi
    @Volatile
    var l0 = Random.nextLong()
    @KotlinxBenchmarkRuntimeInternalApi
    var l1 = l0 + 1L

    @KotlinxBenchmarkRuntimeInternalApi
    @Volatile
    var f0 = Random.nextFloat()
    @KotlinxBenchmarkRuntimeInternalApi
    var f1 = f0 + 1.0f

    @Volatile
    @KotlinxBenchmarkRuntimeInternalApi
    var d0 = Random.nextDouble()
    @KotlinxBenchmarkRuntimeInternalApi
    var d1 = d0 + 1.0

    @KotlinxBenchmarkRuntimeInternalApi
    @Volatile
    var bh: Blackhole? = null

    actual inline fun consume(obj: Any?) {
        // identityHashCode is an intrinsic function
        // resolved into getting an object address, so there will be no call.
        consume(obj.identityHashCode())
    }

    actual inline fun consume(bool: Boolean) {
        consume(bool.toByte())
    }

    actual inline fun consume(c: Char) {
        consume(c.code)
    }

    actual inline fun consume(b: Byte) {
        consume(b.toInt())
    }

    actual inline fun consume(s: Short) {
       consume(s.toInt())
    }

    actual inline fun consume(i: Int) {
        // To ensure that i's value will not be removed by optimizations like dead code elimination,
        // its value is compares with two value i0 and i1, such that i1 = i0 + 1.
        // As long as i0 and i1 are different, the following condition should not ever be met
        // and as the branch following it (note that if it is executed, then NPE will happen).
        // To ensure that at least i0 value will be loaded on every call, it was annotated with Volatile.
        //
        // This approach has one drawback: in general, it should be compiled to a code with two branch instructions,
        // and performance characteristics of a benchmark may not be stable if consumed value is sometimes equal to i0.
        // In practice, there is almost no effect on the measured performance and
        // the difference is within the error margin.
        // However, if it becomes a problem one day, then the condition should be rewritten to something like:
        // if (((i0 xor i) and (i1 xor i)) == -1) { ... }
        // We can't simply compare xor results as it could be optimized to comparison of i0 and i1 and i's evaluation
        // may be sunk into the unreachable branch.
        if ((i0 == i) && (i1 == i)) {
            bh!!.i0 = i
        }
    }

    actual inline fun consume(l: Long) {
        if ((l0 == l) && (l1 == l)) {
            bh!!.l0 = l
        }
    }

    actual inline fun consume(f: Float) {
        if ((f0 == f) && (f1 == f)) {
            bh!!.f0 = f
        }
    }

    actual inline fun consume(d: Double) {
        if ((d0 == d) && (d1 == d)) {
            bh!!.d0 = d
        }
    }
}

@KotlinxBenchmarkRuntimeInternalApi
actual fun Blackhole.flush() = Unit

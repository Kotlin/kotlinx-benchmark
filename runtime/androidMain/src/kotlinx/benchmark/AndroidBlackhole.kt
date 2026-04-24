package kotlinx.benchmark

import androidx.benchmark.*
import kotlinx.benchmark.internal.*

/**
 * Use Jetpack Microbenchmark BlackHole implementation on Android.
 * API is slightly different, so we need to manually map the APIs.
 */
@Suppress("NOTHING_TO_INLINE")
actual class Blackhole {
    actual inline fun consume(obj: Any?) = consume(System.identityHashCode(obj))
    actual inline fun consume(bool: Boolean) = BlackHole.consume(bool)
    actual inline fun consume(c: Char) = BlackHole.consume(c)
    actual inline fun consume(b: Byte) = BlackHole.consume(b)
    actual inline fun consume(s: Short) = BlackHole.consume(s)
    actual inline fun consume(i: Int) = BlackHole.consume(i)
    actual inline fun consume(l: Long) = BlackHole.consume(l)
    actual inline fun consume(f: Float) = BlackHole.consume(f)
    actual inline fun consume(d: Double) = BlackHole.consume(d)
}

@KotlinxBenchmarkRuntimeInternalApi
actual fun Blackhole.flush() = Unit

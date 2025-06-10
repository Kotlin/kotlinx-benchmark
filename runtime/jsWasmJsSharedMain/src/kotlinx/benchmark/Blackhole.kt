package kotlinx.benchmark

import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi

private const val MAGIC_SIZE: Int = 13

@Suppress("NOTHING_TO_INLINE")
public actual class Blackhole {
    private val arrayOfAny: Array<Any?> = arrayOfNulls(MAGIC_SIZE)
    private var currentAnyPosition: Int = 0

    @PublishedApi
    internal fun consumeAny(obj: Any?) {
        arrayOfAny[currentAnyPosition] = obj
        currentAnyPosition = if (currentAnyPosition == MAGIC_SIZE - 1) 0 else currentAnyPosition + 1
    }

    private val arrayOfInt: IntArray = IntArray(MAGIC_SIZE)
    private var currentIntPosition: Int = 0

    @PublishedApi
    internal fun consumeInt(i: Int) {
        arrayOfInt[currentIntPosition] = i
        currentIntPosition = if (currentIntPosition == MAGIC_SIZE - 1) 0 else currentIntPosition + 1
    }

    internal fun flushMe() {
        val sums = arrayOfAny.sumOf { it.hashCode() } + arrayOfInt.sum()
        println("Consumed blackhole value: $sums")
    }

    actual inline fun consume(obj: Any?) = consumeAny(obj)

    actual inline fun consume(bool: Boolean) = consumeInt(bool.hashCode())

    actual inline fun consume(c: Char) = consumeInt(c.hashCode())

    actual inline fun consume(b: Byte) = consumeInt(b.hashCode())

    actual inline fun consume(s: Short) = consumeInt(s.hashCode())

    actual inline fun consume(i: Int) = consumeInt(i.hashCode())

    actual inline fun consume(l: Long) = consumeInt(l.hashCode())

    actual inline fun consume(f: Float) = consumeInt(f.hashCode())

    actual inline fun consume(d: Double) = consumeInt(d.hashCode())
}


@KotlinxBenchmarkRuntimeInternalApi
actual fun Blackhole.flush() = flushMe()

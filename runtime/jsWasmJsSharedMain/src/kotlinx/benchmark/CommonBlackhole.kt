package kotlinx.benchmark

import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi

private const val MAGIC_SIZE: Int = 13

public actual class Blackhole {
    private val arrayOfAny: Array<Any?> = arrayOfNulls(MAGIC_SIZE)
    private var currentAnyPosition: Int = 0
    private fun consumeAny(obj: Any?) {
        arrayOfAny[currentAnyPosition] = obj
        currentAnyPosition = if (currentAnyPosition == MAGIC_SIZE - 1) 0 else currentAnyPosition + 1
    }

    private val arrayOfInt: IntArray = IntArray(MAGIC_SIZE)
    private var currentIntPosition: Int = 0
    private fun consumeInt(i: Int) {
        arrayOfInt[currentIntPosition] = i
        currentIntPosition = if (currentIntPosition == MAGIC_SIZE - 1) 0 else currentIntPosition + 1
    }

    internal fun flushMe() {
        val sums = arrayOfAny.sumOf { it.hashCode() } + arrayOfInt.sum()
        println("Consumed blackhole value: $sums")
    }

    actual fun consume(obj: Any?) = consumeAny(obj)

    actual fun consume(bool: Boolean) = consumeInt(bool.hashCode())

    actual fun consume(c: Char) = consumeInt(c.hashCode())

    actual fun consume(b: Byte) = consumeInt(b.hashCode())

    actual fun consume(s: Short) = consumeInt(s.hashCode())

    actual fun consume(i: Int) = consumeInt(i.hashCode())

    actual fun consume(l: Long) = consumeInt(l.hashCode())

    actual fun consume(f: Float) = consumeInt(f.hashCode())

    actual fun consume(d: Double) = consumeInt(d.hashCode())
}


@KotlinxBenchmarkRuntimeInternalApi
actual fun Blackhole.flush() = flushMe()

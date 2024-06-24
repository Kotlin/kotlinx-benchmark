package kotlinx.benchmark

import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi

private const val MAGIC_SIZE: Int = 13

// TODO: Drop this class once it becomes internal
@KotlinxBenchmarkRuntimeInternalApi
open class CommonBlackhole {
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

    fun flushMe() {
        val sums = arrayOfAny.sumOf { it.hashCode() } + arrayOfInt.sum()
        println("Consumed blackhole value: $sums")
    }

    fun consume(obj: Any?) = consumeAny(obj)

    fun consume(bool: Boolean) = consumeInt(bool.hashCode())

    fun consume(c: Char) = consumeInt(c.hashCode())

    fun consume(b: Byte) = consumeInt(b.hashCode())

    fun consume(s: Short) = consumeInt(s.hashCode())

    fun consume(i: Int) = consumeInt(i.hashCode())

    fun consume(l: Long) = consumeInt(l.hashCode())

    fun consume(f: Float) = consumeInt(f.hashCode())

    fun consume(d: Double) = consumeInt(d.hashCode())
}
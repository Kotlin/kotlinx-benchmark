package kotlinx.benchmark

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

    public actual inline fun consume(obj: Any?): Unit = consumeAny(obj)

    public actual inline fun consume(bool: Boolean): Unit = consumeInt(bool.hashCode())

    public actual inline fun consume(c: Char): Unit = consumeInt(c.hashCode())

    public actual inline fun consume(b: Byte): Unit = consumeInt(b.hashCode())

    public actual inline fun consume(s: Short): Unit = consumeInt(s.hashCode())

    public actual inline fun consume(i: Int): Unit = consumeInt(i.hashCode())

    public actual inline fun consume(l: Long): Unit = consumeInt(l.hashCode())

    public actual inline fun consume(f: Float): Unit = consumeInt(f.hashCode())

    public actual inline fun consume(d: Double): Unit = consumeInt(d.hashCode())
}


internal actual fun Blackhole.flush() = flushMe()

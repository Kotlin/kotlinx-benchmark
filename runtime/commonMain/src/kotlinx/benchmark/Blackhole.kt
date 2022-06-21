package kotlinx.benchmark

expect class Blackhole {
    fun consume(obj: Any?)
    fun consume(bool: Boolean)
    fun consume(c: Char)
    fun consume(b: Byte)
    fun consume(s: Short)
    fun consume(i: Int)
    fun consume(l: Long)
    fun consume(f: Float)
    fun consume(d: Double)
}

expect fun Blackhole.flush()
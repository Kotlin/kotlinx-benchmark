package kotlinx.benchmark


public expect class Blackhole {
    public fun consume(obj: Any?)
    public fun consume(bool: Boolean)
    public fun consume(c: Char)
    public fun consume(b: Byte)
    public fun consume(s: Short)
    public fun consume(i: Int)
    public fun consume(l: Long)
    public fun consume(f: Float)
    public fun consume(d: Double)
}

internal expect fun Blackhole.flush()

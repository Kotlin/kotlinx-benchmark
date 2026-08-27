package kotlinx.benchmark.native


internal expect class Barrier(threads: Int) : AutoCloseable {
    fun wait(): Unit

    override fun close(): Unit
}


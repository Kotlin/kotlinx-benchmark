package kotlinx.benchmark.native

/**
 * A [barrier](https://en.wikipedia.org/wiki/Barrier_(computer_science)), blocking
 * each thread calling [wait] until exactly [threads] number of threads have called [wait].
 *
 * Closing the barrier before joining all the threads that supposed to wait leads to undefined behavior
 * and may lead to runtime errors, crashes, or hangs.
 *
 * @param threads number of threads that must call [wait] before any thread can continue,
 *                the value must be strictly positive.
 */
internal expect class Barrier(threads: Int) : AutoCloseable {
    /**
     * Blocks the calling thread until exactly `threads` number of threads have called [wait].
     *
     * Calling [wait] after all threads met at it will result in a runtime error.
     */
    internal fun wait()

    override fun close()
}

internal inline fun checkError(errorCode: Int, operation: String, onError: () -> Unit = {}) {
    if (errorCode == 0) return
    onError()
    throw IllegalStateException("$operation failed with error $errorCode")
}

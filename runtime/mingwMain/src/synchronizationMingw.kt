package kotlinx.benchmark.native

import kotlinx.cinterop.Arena
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.ptr
import platform.posix.PTHREAD_BARRIER_SERIAL_THREAD
import platform.posix.pthread_barrier_destroy
import platform.posix.pthread_barrier_init
import platform.posix.pthread_barrier_tVar
import platform.posix.pthread_barrier_wait
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndDecrement

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@OptIn(ExperimentalForeignApi::class, ExperimentalAtomicApi::class)
internal actual class Barrier actual constructor(threads: Int) : AutoCloseable {
    private val arena: Arena
    private val barrier: pthread_barrier_tVar
    private var closed = false
    private val remaining = AtomicInt(threads)

    init {
        require(threads > 0) { "threads must be greater than zero, was: $threads" }

        arena = Arena()
        barrier = arena.alloc()
        checkError(
            pthread_barrier_init(barrier.ptr, null, threads.convert()),
            "pthread_barrier_init"
        ) {
            arena.clear()
        }
    }

    @OptIn(ExperimentalAtomicApi::class)
    actual fun wait() {
        check(!closed) { "Barrier was closed already" }
        check(remaining.fetchAndDecrement() > 0) { "Barrier was already reached by the required number of parties." }
        val ret = pthread_barrier_wait(barrier.ptr)
        if (ret != 0 && ret != PTHREAD_BARRIER_SERIAL_THREAD) {
            checkError(ret, "pthread_barrier_wait")
        }
    }

    actual override fun close() {
        if (closed) return
        /* val _ = */ pthread_barrier_destroy(barrier.ptr)
        arena.clear()
        closed = true
    }
}

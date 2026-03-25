package kotlinx.benchmark.native

import kotlinx.cinterop.Arena
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.ptr
import platform.posix.pthread_barrier_destroy
import platform.posix.pthread_barrier_init
import platform.posix.pthread_barrier_tVar
import platform.posix.pthread_barrier_wait

@OptIn(ExperimentalForeignApi::class)
internal actual class Barrier actual constructor(threads: Int) : AutoCloseable {
    private val arena = Arena()
    private val barrier: pthread_barrier_tVar = arena.alloc()
    private var closed = false

    init {
        val ret = pthread_barrier_init(barrier.ptr, null, (threads + 1).convert())
        if (ret != 0) {
            closed = true
            throw IllegalStateException("Failed to initialize pthread_barrier_t: $ret")
        }
    }

    actual fun wait() {
        check(!closed) { "Barrier was closed already" }
        pthread_barrier_wait(barrier.ptr)
    }

    actual override fun close() {
        if (closed) return
        pthread_barrier_destroy(barrier.ptr)
        arena.clear()
        closed = true
    }
}

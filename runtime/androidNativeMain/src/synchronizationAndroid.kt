package kotlinx.benchmark.native

import kotlinx.cinterop.Arena
import kotlinx.cinterop.alloc
import kotlinx.cinterop.ptr
import platform.posix.pthread_cond_broadcast
import platform.posix.pthread_cond_destroy
import platform.posix.pthread_cond_init
import platform.posix.pthread_cond_t
import platform.posix.pthread_cond_wait
import platform.posix.pthread_mutex_destroy
import platform.posix.pthread_mutex_init
import platform.posix.pthread_mutex_lock
import platform.posix.pthread_mutex_t
import platform.posix.pthread_mutex_unlock

internal actual class Barrier actual constructor(private val threads: Int) : AutoCloseable {
    private val arena: Arena
    private var arrived = 0

    private val mutex: pthread_mutex_t
    private val cond: pthread_cond_t

    private var closed = false
    private var allArrived = false

    init {
        require(threads > 0) { "threads must be greater than zero, was: $threads" }

        arena = Arena()
        cond = arena.alloc()
        checkError(pthread_cond_init(cond.ptr, null), "pthread_cond_init") {
            arena.clear()
        }
        mutex = arena.alloc()
        checkError(pthread_mutex_init(mutex.ptr, null), "pthread_mutex_init") {
            /* val _ = */ pthread_cond_destroy(cond.ptr)
            arena.clear()
        }
    }

    actual fun wait() {
        check(!closed) { "Barrier was closed already" }
        check(!allArrived) { "Barrier was already reached by the required number of parties." }

        checkError(pthread_mutex_lock(mutex.ptr), "pthread_mutex_lock")

        arrived++
        if (arrived == threads) {
            checkError(pthread_cond_broadcast(cond.ptr), "pthread_cond_broadcast")
        } else {
            while (arrived < threads) {
                checkError(pthread_cond_wait(cond.ptr, mutex.ptr), "pthread_cond_wait")
            }
        }
        allArrived = true
        checkError(pthread_mutex_unlock(mutex.ptr), "pthread_mutex_unlock")
    }

    actual override fun close() {
        if (closed) return
        /* val _ = */ pthread_cond_destroy(cond.ptr)
        /* val _ = */ pthread_mutex_destroy(mutex.ptr)
        arena.clear()
        closed = true
    }
}

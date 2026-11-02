package kotlinx.benchmark.native

import kotlinx.cinterop.Arena
import kotlinx.cinterop.alloc
import kotlinx.cinterop.ptr
import platform.posix.*

internal actual class Barrier actual constructor(private val threads: Int) : AutoCloseable {
    private val arena: Arena
    private var arrived = 0

    private val mutex: pthread_mutex_t
    private val cond: pthread_cond_t

    private var closed = false
    private val allArrived: Boolean
        get() = arrived >= threads

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

        checkError(pthread_mutex_lock(mutex.ptr), "pthread_mutex_lock")
        if (allArrived) {
            unlockMutexAndThrow(
                IllegalStateException("Barrier was already reached by the required number of parties.")
            ) {
                pthread_mutex_unlock(mutex.ptr)
            }
        }

        arrived++
        if (arrived == threads) {
            checkError(pthread_cond_broadcast(cond.ptr), "pthread_cond_broadcast")
        } else {
            while (arrived < threads) {
                checkErrorUnlockingMutexOnFailure(
                    pthread_cond_wait(cond.ptr, mutex.ptr),
                    "pthread_cond_wait"
                ) {
                    pthread_mutex_unlock(mutex.ptr)
                }
            }
        }
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

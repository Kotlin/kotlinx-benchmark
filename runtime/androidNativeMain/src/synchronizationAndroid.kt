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
    private val arena = Arena()
    private var arrived = 0

    private val mutex: pthread_mutex_t = arena.alloc()
    private val cond: pthread_cond_t = arena.alloc()

    private var closed = false
    private var allArrived = false

    init {
        pthread_cond_init(cond.ptr, null).let {
            if (it != 0) {
                closed = true
                throw IllegalStateException("Failed to initialize pthread_cond_t: $it")
            }
        }
        pthread_mutex_init(mutex.ptr, null).let {
            if (it != 0) {
                closed = true
                pthread_cond_destroy(cond.ptr)
                throw IllegalStateException("Failed to initialize pthread_mutex_t: $it")
            }
        }
    }

    actual fun wait(): Unit {
        check(!closed) { "Barrier was closed already" }
        check(!allArrived) { "Barrier was already reached by the required number of parties." }

        pthread_mutex_lock(mutex.ptr).let {
            check(it == 0) { "Lock acquisition failed: $it" }
        }
        arrived++
        if (arrived == threads) {
            pthread_cond_broadcast(cond.ptr).let {
                if (it != 0) throw IllegalStateException("Cond var broadcast failed: $it")
            }
        } else {
            while (arrived < threads) {
                pthread_cond_wait(cond.ptr, mutex.ptr).let {
                    if (it != 0) throw IllegalStateException("Cond wait failed: $it")
                }
            }
        }
        allArrived = true
        pthread_mutex_unlock(mutex.ptr).let {
            if (it != 0) throw IllegalStateException("Unlock failed: $it")
        }
    }

    actual override fun close() {
        if (closed) return
        pthread_cond_destroy(cond.ptr)
        pthread_mutex_destroy(mutex.ptr)
        arena.clear()
        closed = true
    }
}

package kotlinx.benchmark.native

import kotlinx.cinterop.*
import kotlinx.cinterop.Arena
import platform.posix.EINTR
import platform.posix.errno
import platform.posix.nanosleep
import platform.posix.timespec
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Creates a wrapper around `nanosleep (2)` function,
 * that will fall asleep for the given [duration] on once [sleep] is called.
 *
 * Object creation does initialization and memory allocation, but does not perform the sleep itself.
 *
 * Impl note: it works this way to exclude arena allocation time from the sleep itself.
 */
@OptIn(UnsafeNumber::class)
internal class Nanosleep(duration: Duration) : AutoCloseable {
    private val arena: Arena
    private val timespecInitial: timespec
    private val timespecRemaining: timespec
    private var alreadySlept: Boolean = false
    private var closed: Boolean = false

    init {
        require(duration.isPositive() && duration.isFinite()) { "Duration must be positive and finite, was: $duration" }
        val remainingNs = duration.inWholeNanoseconds - duration.inWholeSeconds.seconds.inWholeNanoseconds
        require(remainingNs in 0..<1_000_000_000) {
            "Incorrect remainingNs value: $remainingNs"
        }

        arena = Arena()
        timespecInitial = arena.alloc()
        timespecRemaining = arena.alloc()
        timespecInitial.tv_sec = duration.inWholeSeconds.convert()
        timespecInitial.tv_nsec = remainingNs.convert()
    }

    /**
     * Sleeps for the duration specified during this [Nanosleep] instance construction.
     */
    fun sleep() {
        check(!alreadySlept) { "Sleep was already called." }
        check(!closed) { "Nanosleep instance is closed." }
        alreadySlept = true
        while (true) {
            val ret = nanosleep(timespecInitial.ptr, timespecRemaining.ptr)
            if (ret == 0) break
            val err = errno
            if (ret == -1 && err == EINTR /* EINVAL can't happen, EFAULT we can't handle */) {
                if (timespecRemaining.tv_sec.convert<Long>() == 0L &&
                    timespecRemaining.tv_nsec.convert<Long>() == 0L
                ) break
                timespecInitial.tv_sec = timespecRemaining.tv_sec
                timespecInitial.tv_nsec = timespecRemaining.tv_nsec
                continue
            }
            throw IllegalStateException("nanosleep failed: $ret, errno: $err")
        }
    }

    override fun close() {
        closed = true
        arena.clear()
    }
}

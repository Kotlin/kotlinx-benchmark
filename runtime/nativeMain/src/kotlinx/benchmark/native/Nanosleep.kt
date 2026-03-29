package kotlinx.benchmark.native

import kotlinx.cinterop.Arena
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.ptr
import platform.posix.nanosleep
import platform.posix.timespec
import kotlin.ranges.contains
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(UnsafeNumber::class, ExperimentalForeignApi::class)
internal class Nanosleep(duration: Duration) : AutoCloseable {
    val arena = Arena()
    val timespecInitial: timespec = arena.alloc()
    val timespecRemaining: timespec = arena.alloc()

    init {
        require(duration.isPositive())

        timespecInitial.tv_sec = duration.inWholeSeconds.convert()
        val remainingNs = duration.inWholeNanoseconds - duration.inWholeSeconds.seconds.inWholeNanoseconds
        require(remainingNs in 0..< 1_000_000_000) {
            "Incorrect remainingNs value: $remainingNs"
        }
        timespecInitial.tv_nsec = remainingNs.convert()
    }

    fun sleep() {
        while (true) {
            val ret = nanosleep(timespecInitial.ptr, timespecRemaining.ptr)
            if (ret == 0) break
            if (ret == -1) {
                if (timespecRemaining.tv_sec.convert<Long>() == 0L &&
                    timespecRemaining.tv_nsec.convert<Long>() == 0L) break
                timespecInitial.tv_sec = timespecRemaining.tv_sec
                timespecInitial.tv_nsec = timespecRemaining.tv_nsec
                continue
            }
            throw IllegalStateException("nanosleep failed: $ret")
        }
    }

    override fun close() {
        arena.clear()
    }

}

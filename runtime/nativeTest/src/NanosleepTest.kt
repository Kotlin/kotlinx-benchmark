package kotlinx.benchmark.tests

import kotlinx.benchmark.native.Nanosleep
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

class NanosleepTest {
    @Test
    fun sleepTest() {
        val sleepDuration = Nanosleep(1.seconds).use {
            measureTime {
                it.sleep()
            }
        }
        assertTrue(
            sleepDuration >= 1.seconds && sleepDuration < 2.seconds,
            "Actual sleep duration: $sleepDuration"
        )
    }
}

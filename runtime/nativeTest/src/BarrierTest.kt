package kotlinx.benchmark.tests

import kotlinx.benchmark.native.Barrier
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import platform.posix.usleep
import kotlin.concurrent.AtomicInt
import kotlin.native.concurrent.ObsoleteWorkersApi
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.test.Test
import kotlin.test.assertEquals

class BarrierTest {
    private data class TestState(
        val barrier: Barrier,
        val preBarrierCounter: AtomicInt,
        val postBarrierCounter: AtomicInt
    )

    @OptIn(ObsoleteWorkersApi::class, ExperimentalForeignApi::class)
    @Test
    fun testBarrier() {
        val threads = 2
        val testState = TestState(Barrier(threads + 1), AtomicInt(0), AtomicInt(0))

        val workers = Array(threads) { Worker.start() }
        try {
            val futures = workers.map { worker ->
                worker.execute(TransferMode.UNSAFE, { testState }) {
                    it.preBarrierCounter.incrementAndGet()
                    it.barrier.wait()
                    it.postBarrierCounter.incrementAndGet()
                }
            }

            while (testState.preBarrierCounter.value != threads) {
                usleep(100.convert())
            }

            assertEquals(threads, testState.preBarrierCounter.value)
            assertEquals(0, testState.postBarrierCounter.value)
            testState.barrier.wait()

            while (testState.postBarrierCounter.value != threads) {
                usleep(100.convert())
            }

            futures.forEach { it.result }

        } finally {
            testState.barrier.close()
            for (worker in workers) {
                worker.requestTermination(false).result
            }
        }
    }
}

package kotlinx.benchmark.tests

import kotlinx.benchmark.native.Barrier
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import platform.posix.usleep
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.native.concurrent.ObsoleteWorkersApi
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

@OptIn(ExperimentalAtomicApi::class)
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
                    it.preBarrierCounter.incrementAndFetch()
                    it.barrier.wait()
                    it.postBarrierCounter.incrementAndFetch()
                }
            }

            while (testState.preBarrierCounter.load() != threads) {
                usleep(100.convert())
            }

            assertEquals(threads, testState.preBarrierCounter.load())
            assertEquals(0, testState.postBarrierCounter.load())
            testState.barrier.wait()

            while (testState.postBarrierCounter.load() != threads) {
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

    @Test
    fun testWaitAfterAllPariesMet() {
        Barrier(1).use {
            it.wait()
            assertFails { it.wait() }
        }
    }
}

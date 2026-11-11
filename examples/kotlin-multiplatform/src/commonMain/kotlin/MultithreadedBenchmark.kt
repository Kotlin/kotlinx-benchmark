package test

import kotlinx.benchmark.*
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

@OptIn(ExperimentalAtomicApi::class)
// run two instances of the benchmark concurrently,
// with a state shared between them
@Threads(2)
@State(Scope.Benchmark)
@Measurement(iterations = 3, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
class MultithreadedBenchmark {
    val atomicCounter = AtomicLong(0)

    @Benchmark
    fun increment(bh: Blackhole) {
        bh.consume(atomicCounter.incrementAndFetch())
    }
}

package test

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Blackhole
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import kotlinx.benchmark.Threads
import kotlin.concurrent.Volatile

@State(Scope.Benchmark)
@Threads(2)
open class MultithreadedBenchmark {
    @Volatile private var counter = 0

    @Benchmark
    fun concurrentIncrement(bh: Blackhole) {
        bh.consume(counter++)
    }
}

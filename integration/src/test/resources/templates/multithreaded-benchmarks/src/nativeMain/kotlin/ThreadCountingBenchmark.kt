package test

import kotlinx.benchmark.*
import kotlin.math.*
import kotlin.concurrent.atomics.*
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.ObsoleteWorkersApi

@OptIn(ExperimentalAtomicApi::class, ObsoleteWorkersApi::class)
@State(Scope.Benchmark)
@Warmup(iterations = 1, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
@Measurement(iterations = 1, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
@Threads(2)
open class ThreadCountingBenchmark {
    val observedWorkers = AtomicReference<List<Long>>(emptyList())

    @Benchmark
    open fun doTheMath(): Unit {
        val wid = Worker.current.id.toLong()

        observedWorkers.fetchAndUpdate { prev ->
            if (prev.contains(wid)) prev else prev + wid
        }
    }

    @Setup
    fun clearList() {
        observedWorkers.store(emptyList())
    }

    @TearDown
    fun checkThreads() {
        val list = observedWorkers.load()
        require(2 == list.distinct().size) {
            "Two workers were expected, but found $list"
        }
    }
}

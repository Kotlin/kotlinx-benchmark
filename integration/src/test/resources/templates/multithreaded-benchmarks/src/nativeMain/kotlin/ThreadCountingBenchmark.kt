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
    val map = AtomicReference<Map<Long, Long>>(emptyMap())

    @Benchmark
    open fun doTheMath(): Unit {
        val wid = Worker.current.id.toLong()
        map.fetchAndUpdate { prev ->
            val oldCounter = prev.getOrElse(wid) { 0L }
            buildMap {
                putAll(prev)
                put(wid, oldCounter + 1L)
            }
        }
    }

    @Setup
    fun clearMap() {
        map.store(emptyMap())
    }

    @TearDown
    fun checkThreads() {
        require(2 == map.load().size) {
            "Two workers were expected, but found ${map.load()}"
        }
    }
}

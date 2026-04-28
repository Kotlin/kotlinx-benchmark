package test

import kotlinx.benchmark.*
import kotlin.concurrent.atomics.*
import kotlin.math.*

@OptIn(ExperimentalAtomicApi::class)
@State(Scope.Benchmark)
@Warmup(iterations = 1, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
@Measurement(iterations = 1, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
@Threads(2)
open class ThreadCountingBenchmark {
    val map = AtomicReference<Map<Long, AtomicLong>>(emptyMap())

    @Benchmark
    open fun doTheMath() {
        do {
            val prev = map.load()
            val tid = Thread.currentThread().id
            if (tid in prev) {
                prev.getValue(tid).incrementAndFetch()
                return
            } else {
                val newMap = buildMap {
                    putAll(prev)
                    put(tid, AtomicLong(1L))
                }
                if (map.compareAndSet(prev, newMap)) return
            }
        } while (true)
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

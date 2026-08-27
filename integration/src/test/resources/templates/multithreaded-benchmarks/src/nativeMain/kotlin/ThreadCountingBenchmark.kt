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
    val map = AtomicReference<Map<Long, AtomicLong>>(emptyMap())

    @Benchmark
    open fun doTheMath(): Unit {
        do {
            val prev = map.load()
            val wid = Worker.current.id.toLong()
            if (wid in prev) {
                prev.getValue(wid).incrementAndFetch()
                return
            } else {
                val newMap = buildMap {
                    putAll(prev)
                    put(wid, AtomicLong(1L))
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

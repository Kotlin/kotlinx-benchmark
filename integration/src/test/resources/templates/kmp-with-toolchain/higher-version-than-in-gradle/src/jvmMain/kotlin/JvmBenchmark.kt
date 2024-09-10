package test

import kotlinx.benchmark.*

// Don't really need to measure anything here, just check that the benchmark works
@State(Scope.Benchmark)
@Warmup(iterations = 0)
@Measurement(iterations = 1, time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
open class JvmBenchmark {
    private val arrayList = java.util.ArrayList<Int>()

    @Setup
    fun setup() {
        // Methods from JDK21
        arrayList.addFirst(0)
        arrayList.addLast(1)
    }

    @Benchmark
    open fun benchmark() = arrayList.size
}

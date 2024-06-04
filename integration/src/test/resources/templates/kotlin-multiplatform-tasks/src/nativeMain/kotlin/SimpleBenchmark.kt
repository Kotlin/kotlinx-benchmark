package test

import kotlinx.benchmark.*
import kotlin.math.*
import test.*

@State(Scope.Benchmark)
@Measurement(iterations = 3, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
open class SimpleBenchmark {

    @Benchmark
    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
    fun cinterop(): Int = foo()

    @Benchmark
    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
    open fun mathBenchmark(): Double {
        return log(sqrt(3.0) * cos(3.0), 2.0) * foo()
    }
}

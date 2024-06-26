package test

import kotlinx.benchmark.*
import kotlin.native.Platform

@State(Scope.Benchmark)
@Measurement(iterations = 3, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
open class DebugBenchmark {
    @Benchmark
    @OptIn(kotlin.experimental.ExperimentalNativeApi::class)
    open fun debugBenchmark(): String {
        return if (Platform.isDebugBinary) "Debug Benchmark"
        else error("Not a debug binary")
    }
}

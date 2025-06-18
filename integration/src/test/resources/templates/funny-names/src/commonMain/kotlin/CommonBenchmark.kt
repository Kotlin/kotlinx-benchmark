package test

import kotlinx.benchmark.*
import kotlin.jvm.*

@JvmInline
value class StringWrapper(val value: String)

@State(Scope.Benchmark)
@Measurement(iterations = 1, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
@Warmup(iterations = 1, time = 500, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
open class CommonBenchmark {
    @Benchmark // will be wrapString-something on JVM
    fun wrapString() = StringWrapper("Hello World!")

    @Benchmark
    @JvmName("-explicitlyRenamed")
    fun explicitlyRenamed() = 0

    @Benchmark
    fun `backticked name`() = 0

    @Benchmark
    fun `assert`() = 0
}

abstract class BenchmarkBase {
    @Benchmark
    @JvmName("-illegal base name")
    fun base() = 0
}

@State(Scope.Benchmark)
@Measurement(iterations = 1, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
@Warmup(iterations = 1, time = 500, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
open class ConcreteBenchmark : BenchmarkBase()

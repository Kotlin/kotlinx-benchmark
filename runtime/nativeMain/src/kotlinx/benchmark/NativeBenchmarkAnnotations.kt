package kotlinx.benchmark

actual enum class Scope {
    Benchmark
}

actual annotation class State(actual val value: Scope)
actual annotation class Setup
actual annotation class TearDown
actual annotation class Benchmark


@Target(AnnotationTarget.CLASS)
actual annotation class BenchmarkMode(actual vararg val value: Mode)

actual enum class Mode {
    Throughput, AverageTime
}

enum class NativeFork {
    PerBenchmark, PerIteration
}

@Target(AnnotationTarget.CLASS)
actual annotation class OutputTimeUnit(actual val value: BenchmarkTimeUnit)

actual enum class BenchmarkTimeUnit {
    NANOSECONDS, MICROSECONDS, MILLISECONDS, SECONDS, MINUTES
}

@Target(AnnotationTarget.CLASS)
actual annotation class Warmup(
    actual val iterations: Int
)

@Target(AnnotationTarget.CLASS)
actual annotation class Measurement(
    actual val iterations: Int,
    actual val time: Int,
    actual val timeUnit: BenchmarkTimeUnit,
    actual val batchSize: Int
)

actual annotation class Param(actual vararg val value: String)
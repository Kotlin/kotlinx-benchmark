package kotlinx.benchmark

import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi

@Target(AnnotationTarget.FUNCTION)
expect annotation class Setup()

@Target(AnnotationTarget.FUNCTION)
expect annotation class TearDown()

@Target(AnnotationTarget.FUNCTION)
expect annotation class Benchmark()

@Target(AnnotationTarget.CLASS)
expect annotation class State(val value: Scope)

expect enum class Scope {
    Benchmark
}

@Target(AnnotationTarget.CLASS)
expect annotation class BenchmarkMode(vararg val value: Mode)

expect enum class Mode {
    Throughput, AverageTime
}

@Target(AnnotationTarget.CLASS)
expect annotation class OutputTimeUnit(val value: BenchmarkTimeUnit)

expect enum class BenchmarkTimeUnit {
    NANOSECONDS, MICROSECONDS, MILLISECONDS, SECONDS, MINUTES
}

@KotlinxBenchmarkRuntimeInternalApi
@Suppress("REDUNDANT_ELSE_IN_WHEN")
fun BenchmarkTimeUnit.toText() = when (this) {
    BenchmarkTimeUnit.NANOSECONDS -> "ns"
    BenchmarkTimeUnit.MICROSECONDS -> "us"
    BenchmarkTimeUnit.MILLISECONDS -> "ms"
    BenchmarkTimeUnit.SECONDS -> "sec"
    BenchmarkTimeUnit.MINUTES -> "min"
    else -> throw UnsupportedOperationException("$this is not supported")
}

@KotlinxBenchmarkRuntimeInternalApi
fun String.toMode() =
    when (this) {
        "thrpt", "Throughput" -> Mode.Throughput
        "avgt", "AverageTime" -> Mode.AverageTime
        else -> throw UnsupportedOperationException("$this is not supported")
    }


@KotlinxBenchmarkRuntimeInternalApi
@Suppress("REDUNDANT_ELSE_IN_WHEN")
fun Mode.toText() = when (this) {
    Mode.Throughput -> "thrpt"
    Mode.AverageTime -> "avgt"
    else -> throw UnsupportedOperationException("$this is not supported")
}

@KotlinxBenchmarkRuntimeInternalApi
@Suppress("REDUNDANT_ELSE_IN_WHEN")
fun BenchmarkTimeUnit.toMultiplier() = when (this) {
    BenchmarkTimeUnit.NANOSECONDS -> 1
    BenchmarkTimeUnit.MICROSECONDS -> 1_000
    BenchmarkTimeUnit.MILLISECONDS -> 1_000_000
    BenchmarkTimeUnit.SECONDS -> 1_000_000_000
    BenchmarkTimeUnit.MINUTES -> 60_000_000_000
    else -> throw UnsupportedOperationException("$this is not supported")
}

@KotlinxBenchmarkRuntimeInternalApi
@Suppress("REDUNDANT_ELSE_IN_WHEN")
fun BenchmarkTimeUnit.toSecondsMultiplier() = when (this) {
    BenchmarkTimeUnit.NANOSECONDS -> 1.0 / 1_000_000_000
    BenchmarkTimeUnit.MICROSECONDS -> 1.0 / 1_000_000
    BenchmarkTimeUnit.MILLISECONDS -> 1.0 / 1_000
    BenchmarkTimeUnit.SECONDS -> 1.0
    BenchmarkTimeUnit.MINUTES -> 60.0
    else -> throw UnsupportedOperationException("$this is not supported")
}

@Target(AnnotationTarget.CLASS)
expect annotation class Warmup(
    val iterations: Int = -1,
    val time: Int = -1,
    val timeUnit: BenchmarkTimeUnit = BenchmarkTimeUnit.SECONDS,
    val batchSize: Int = -1
)

@Target(AnnotationTarget.CLASS)
expect annotation class Measurement(
    val iterations: Int = -1,
    val time: Int = -1,
    val timeUnit: BenchmarkTimeUnit = BenchmarkTimeUnit.SECONDS,
    val batchSize: Int = -1
)

expect annotation class Param(vararg val value: String)
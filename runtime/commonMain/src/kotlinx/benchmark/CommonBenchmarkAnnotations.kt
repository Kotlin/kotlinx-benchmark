package kotlinx.benchmark

@Target(AnnotationTarget.FUNCTION)
public expect annotation class Setup()

@Target(AnnotationTarget.FUNCTION)
public expect annotation class TearDown()

@Target(AnnotationTarget.FUNCTION)
public expect annotation class Benchmark()

@Target(AnnotationTarget.CLASS)
public expect annotation class State(val value: Scope)

public expect enum class Scope {
    Benchmark
}

@Target(AnnotationTarget.CLASS)
public expect annotation class BenchmarkMode(vararg val value: Mode)

public expect enum class Mode {
    Throughput, AverageTime
}

@Target(AnnotationTarget.CLASS)
public expect annotation class OutputTimeUnit(val value: BenchmarkTimeUnit)

public expect enum class BenchmarkTimeUnit {
    NANOSECONDS, MICROSECONDS, MILLISECONDS, SECONDS, MINUTES
}

@Suppress("REDUNDANT_ELSE_IN_WHEN")
internal fun BenchmarkTimeUnit.toText() = when (this) {
    BenchmarkTimeUnit.NANOSECONDS -> "ns"
    BenchmarkTimeUnit.MICROSECONDS -> "us"
    BenchmarkTimeUnit.MILLISECONDS -> "ms"
    BenchmarkTimeUnit.SECONDS -> "sec"
    BenchmarkTimeUnit.MINUTES -> "min"
    else -> throw UnsupportedOperationException("$this is not supported")
}

internal fun String.toMode() =
    when (this) {
        "thrpt", "Throughput" -> Mode.Throughput
        "avgt", "AverageTime" -> Mode.AverageTime
        else -> throw UnsupportedOperationException("$this is not supported")
    }

@Suppress("REDUNDANT_ELSE_IN_WHEN")
internal fun Mode.toText() = when (this) {
    Mode.Throughput -> "thrpt"
    Mode.AverageTime -> "avgt"
    else -> throw UnsupportedOperationException("$this is not supported")
}

@Suppress("REDUNDANT_ELSE_IN_WHEN")
internal fun BenchmarkTimeUnit.toMultiplier() = when (this) {
    BenchmarkTimeUnit.NANOSECONDS -> 1
    BenchmarkTimeUnit.MICROSECONDS -> 1_000
    BenchmarkTimeUnit.MILLISECONDS -> 1_000_000
    BenchmarkTimeUnit.SECONDS -> 1_000_000_000
    BenchmarkTimeUnit.MINUTES -> 60_000_000_000
    else -> throw UnsupportedOperationException("$this is not supported")
}

@Suppress("REDUNDANT_ELSE_IN_WHEN")
internal fun BenchmarkTimeUnit.toSecondsMultiplier() = when (this) {
    BenchmarkTimeUnit.NANOSECONDS -> 1.0 / 1_000_000_000
    BenchmarkTimeUnit.MICROSECONDS -> 1.0 / 1_000_000
    BenchmarkTimeUnit.MILLISECONDS -> 1.0 / 1_000
    BenchmarkTimeUnit.SECONDS -> 1.0
    BenchmarkTimeUnit.MINUTES -> 60.0
    else -> throw UnsupportedOperationException("$this is not supported")
}

@Target(AnnotationTarget.CLASS)
public expect annotation class Warmup(
    val iterations: Int = -1,
    val time: Int = -1,
    val timeUnit: BenchmarkTimeUnit = BenchmarkTimeUnit.SECONDS,
    val batchSize: Int = -1
)

@Target(AnnotationTarget.CLASS)
public expect annotation class Measurement(
    val iterations: Int = -1,
    val time: Int = -1,
    val timeUnit: BenchmarkTimeUnit = BenchmarkTimeUnit.SECONDS,
    val batchSize: Int = -1
)

public expect annotation class Param(vararg val value: String)

/**
 * Specifies the number of threads that a benchmark function will be executed in.
 *
 * [value] has to be either strictly positive (> 0), or it could be a special constant [THREADS_CPU_COUNT].
 */
@Target(AnnotationTarget.CLASS)
public expect annotation class Threads(val value: Int)

/**
 * A special value for [Threads.value] forcing to run a benchmark using `N`-threads,
 * where `N` is the total number of CPUs available in runtime.
 */
public const val THREADS_CPU_COUNT: Int = -1

package kotlinx.benchmark

public actual enum class Scope {
    Benchmark
}

@Target(AnnotationTarget.CLASS)
public actual annotation class State(actual val value: Scope)

@Target(AnnotationTarget.FUNCTION)
public actual annotation class Setup

@Target(AnnotationTarget.FUNCTION)
public actual annotation class TearDown

@Target(AnnotationTarget.FUNCTION)
public actual annotation class Benchmark


@Target(AnnotationTarget.CLASS)
public actual annotation class BenchmarkMode(actual vararg val value: Mode)

public actual enum class Mode {
    Throughput, AverageTime
}

internal enum class NativeFork {
    PerBenchmark, PerIteration
}

@Target(AnnotationTarget.CLASS)
public actual annotation class OutputTimeUnit(actual val value: BenchmarkTimeUnit)

public actual enum class BenchmarkTimeUnit {
    NANOSECONDS, MICROSECONDS, MILLISECONDS, SECONDS, MINUTES
}

@Target(AnnotationTarget.CLASS)
public actual annotation class Warmup(
    actual val iterations: Int,
    actual val time: Int,
    actual val timeUnit: BenchmarkTimeUnit,
    actual val batchSize: Int
)

@Target(AnnotationTarget.CLASS)
public actual annotation class Measurement(
    actual val iterations: Int,
    actual val time: Int,
    actual val timeUnit: BenchmarkTimeUnit,
    actual val batchSize: Int
)

public actual annotation class Param(actual vararg val value: String)

@Target(AnnotationTarget.CLASS)
public actual annotation class Threads(actual val value: Int)

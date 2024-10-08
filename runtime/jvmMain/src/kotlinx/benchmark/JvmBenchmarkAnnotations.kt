package kotlinx.benchmark

actual typealias Scope = org.openjdk.jmh.annotations.Scope

actual typealias State = org.openjdk.jmh.annotations.State

actual typealias Setup = org.openjdk.jmh.annotations.Setup

actual typealias TearDown = org.openjdk.jmh.annotations.TearDown

actual typealias Benchmark = org.openjdk.jmh.annotations.Benchmark

actual typealias BenchmarkMode = org.openjdk.jmh.annotations.BenchmarkMode

actual typealias Mode = org.openjdk.jmh.annotations.Mode

actual typealias OutputTimeUnit = org.openjdk.jmh.annotations.OutputTimeUnit

actual typealias BenchmarkTimeUnit = java.util.concurrent.TimeUnit

actual typealias Warmup = org.openjdk.jmh.annotations.Warmup

@Suppress("ACTUAL_ANNOTATION_CONFLICTING_DEFAULT_ARGUMENT_VALUE")
actual typealias Measurement = org.openjdk.jmh.annotations.Measurement

actual typealias Param = org.openjdk.jmh.annotations.Param
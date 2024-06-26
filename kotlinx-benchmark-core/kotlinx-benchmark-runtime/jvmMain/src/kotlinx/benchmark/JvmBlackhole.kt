package kotlinx.benchmark

import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi

actual typealias Blackhole = org.openjdk.jmh.infra.Blackhole

@KotlinxBenchmarkRuntimeInternalApi
actual fun Blackhole.flush() = Unit

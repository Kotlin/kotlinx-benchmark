package kotlinx.benchmark

import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi

@OptIn(KotlinxBenchmarkRuntimeInternalApi::class)
actual class Blackhole : CommonBlackhole()

@KotlinxBenchmarkRuntimeInternalApi
actual fun Blackhole.flush() = flushMe()
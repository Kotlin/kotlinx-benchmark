package kotlinx.benchmark

import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi

actual class Blackhole : CommonBlackhole()

@KotlinxBenchmarkRuntimeInternalApi
actual fun Blackhole.flush() = flushMe()
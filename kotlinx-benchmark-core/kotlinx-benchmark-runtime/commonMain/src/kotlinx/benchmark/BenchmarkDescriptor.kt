package kotlinx.benchmark

import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi

@KotlinxBenchmarkRuntimeInternalApi
abstract class BenchmarkDescriptor<T>(
    val name: String,
    val suite: SuiteDescriptor<T>,
    val blackhole: Blackhole,
)

@KotlinxBenchmarkRuntimeInternalApi
open class BenchmarkDescriptorWithNoBlackholeParameter<T>(
    name: String,
    suite: SuiteDescriptor<T>,
    blackhole: Blackhole,
    val function: T.() -> Any?,
) : BenchmarkDescriptor<T>(name, suite, blackhole)

@KotlinxBenchmarkRuntimeInternalApi
open class BenchmarkDescriptorWithBlackholeParameter<T>(
    name: String,
    suite: SuiteDescriptor<T>,
    blackhole: Blackhole,
    val function: T.(Blackhole) -> Any?,
) : BenchmarkDescriptor<T>(name, suite, blackhole)

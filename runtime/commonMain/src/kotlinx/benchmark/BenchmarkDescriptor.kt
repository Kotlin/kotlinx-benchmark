package kotlinx.benchmark

import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi

@KotlinxBenchmarkRuntimeInternalApi
public abstract class BenchmarkDescriptor<T>(
    public val name: String,
    public val suite: SuiteDescriptor<T>,
    public val blackhole: Blackhole,
)

@KotlinxBenchmarkRuntimeInternalApi
public open class BenchmarkDescriptorWithNoBlackholeParameter<T>(
    name: String,
    suite: SuiteDescriptor<T>,
    blackhole: Blackhole,
    public val function: T.() -> Any?,
) : BenchmarkDescriptor<T>(name, suite, blackhole)

@KotlinxBenchmarkRuntimeInternalApi
public open class BenchmarkDescriptorWithBlackholeParameter<T>(
    name: String,
    suite: SuiteDescriptor<T>,
    blackhole: Blackhole,
    public val function: T.(Blackhole) -> Any?,
) : BenchmarkDescriptor<T>(name, suite, blackhole)

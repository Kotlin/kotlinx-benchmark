package kotlinx.benchmark

abstract class BenchmarkDescriptor<T>(
    val name: String,
    val suite: SuiteDescriptor<T>,
    val blackhole: Blackhole,
)

open class BenchmarkDescriptorWithNoBlackholeParameter<T>(
    name: String,
    suite: SuiteDescriptor<T>,
    blackhole: Blackhole,
    val function: T.() -> Any?,
) : BenchmarkDescriptor<T>(name, suite, blackhole)

open class BenchmarkDescriptorWithBlackholeParameter<T>(
    name: String,
    suite: SuiteDescriptor<T>,
    blackhole: Blackhole,
    val function: T.(Blackhole) -> Any?,
) : BenchmarkDescriptor<T>(name, suite, blackhole)

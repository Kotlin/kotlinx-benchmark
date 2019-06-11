package kotlinx.benchmark

open class BenchmarkDescriptor<T>(
    val name: String,
    val suite: SuiteDescriptor<T>,
    val function: T.() -> Any?
)
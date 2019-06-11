package kotlinx.benchmark.native

import kotlinx.benchmark.*

class NativeBenchmarkDescriptor<T>(
    name: String,
    suite: SuiteDescriptor<T>,
    function: T.() -> Any?
) : BenchmarkDescriptor<T>(name, suite, function) 
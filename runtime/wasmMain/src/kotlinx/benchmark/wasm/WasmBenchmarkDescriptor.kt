package kotlinx.benchmark.wasm

import kotlinx.benchmark.*

class WasmBenchmarkDescriptor<T>(
    name: String,
    suite: SuiteDescriptor<T>,
    function: T.() -> Any?
) : BenchmarkDescriptor<T>(name, suite, function) 
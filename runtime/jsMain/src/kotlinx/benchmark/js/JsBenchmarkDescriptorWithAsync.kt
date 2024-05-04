package kotlinx.benchmark.js

import kotlinx.benchmark.*
import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi
import kotlin.js.*

@KotlinxBenchmarkRuntimeInternalApi
class JsBenchmarkDescriptorWithNoBlackholeParameter<T>(
    name: String,
    suite: SuiteDescriptor<T>,
    blackhole: Blackhole,
    function: T.() -> Any?,
    val async: Boolean = false
) : BenchmarkDescriptorWithNoBlackholeParameter<T>(name, suite, blackhole, function) {
    constructor(
        name: String,
        suite: SuiteDescriptor<T>,
        blackhole: Blackhole,
        function: T.() -> Promise<*>
    ) : this(name, suite, blackhole, function, true)
}

@KotlinxBenchmarkRuntimeInternalApi
class JsBenchmarkDescriptorWithBlackholeParameter<T>(
    name: String,
    suite: SuiteDescriptor<T>,
    blackhole: Blackhole,
    function: T.(Blackhole) -> Any?,
    val async: Boolean = false
) : BenchmarkDescriptorWithBlackholeParameter<T>(name, suite, blackhole, function) {
    constructor(
        name: String,
        suite: SuiteDescriptor<T>,
        blackhole: Blackhole,
        function: T.(Blackhole) -> Promise<*>
    ) : this(name, suite, blackhole, function, true)
}

@OptIn(KotlinxBenchmarkRuntimeInternalApi::class)
internal val BenchmarkDescriptor<*>.isAsync get() = when (this) {
    is JsBenchmarkDescriptorWithNoBlackholeParameter<*> -> async
    is JsBenchmarkDescriptorWithBlackholeParameter<*> -> async
    else -> error("Unexpected ${this::class.simpleName}")
}
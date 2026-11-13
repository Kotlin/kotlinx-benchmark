package kotlinx.benchmark.js

import kotlinx.benchmark.*
import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi
import kotlin.js.*

@KotlinxBenchmarkRuntimeInternalApi
public class JsBenchmarkDescriptorWithNoBlackholeParameter<T>(
    name: String,
    suite: SuiteDescriptor<T>,
    blackhole: Blackhole,
    function: T.() -> Any?,
    public val async: Boolean = false
) : BenchmarkDescriptorWithNoBlackholeParameter<T>(name, suite, blackhole, function) {
    public constructor(
        name: String,
        suite: SuiteDescriptor<T>,
        blackhole: Blackhole,
        function: T.() -> Promise<*>
    ) : this(name, suite, blackhole, function, true)
}

@KotlinxBenchmarkRuntimeInternalApi
public class JsBenchmarkDescriptorWithBlackholeParameter<T>(
    name: String,
    suite: SuiteDescriptor<T>,
    blackhole: Blackhole,
    function: T.(Blackhole) -> Any?,
    public val async: Boolean = false
) : BenchmarkDescriptorWithBlackholeParameter<T>(name, suite, blackhole, function) {
    public constructor(
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

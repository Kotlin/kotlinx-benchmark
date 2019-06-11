package kotlinx.benchmark.js

import kotlinx.benchmark.*
import kotlin.js.*

class JsBenchmarkDescriptor<T>(
    name: String,
    suite: SuiteDescriptor<T>,
    function: T.() -> Any?,
    val async: Boolean = false
) : BenchmarkDescriptor<T>(name, suite, function) {

    constructor(
        name: String,
        suite: SuiteDescriptor<T>,
        function: T.() -> Promise<*>
    ) : this(name, suite, function, true)
}
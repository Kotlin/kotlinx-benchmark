package org.jetbrains.gradle.benchmarks.native

import org.jetbrains.gradle.benchmarks.*

class NativeBenchmarkDescriptor<T>(
    name: String,
    suite: SuiteDescriptor<T>,
    function: T.() -> Any?
) : BenchmarkDescriptor<T>(name, suite, function) 
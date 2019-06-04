package org.jetbrains.gradle.benchmarks

import kotlinx.cli.*

class RunnerConfiguration : CommandLineInterface("Client") {
    val name by onFlagValue("-n", "name", "Name of the configuration").store()
    val filter by onFlagValue("-f", "filter", "Filter for benchmarks, matched by substring").store()
    val reportFile by onFlagValue("-r", "reportFile", "File to save report to").store()
    val traceFormat by onFlagValue("-t", "traceFormat", "Format of tracing report (text or xml)").store("text")

    val iterations by onFlagValue("-i", "iterations", "Number of iterations per benchmark").map { it.toInt() }.store()
    val warmups by onFlagValue("-w", "warmups", "Number of warmup iterations per benchmark").map { it.toInt() }.store()
    
    val iterationTime by onFlagValue(
        "-it",
        "iterationTime",
        "Time to run one iteration in milliseconds"
    ).map { it.toLong() }.store()
    
    val iterationTimeUnit by onFlagValue(
        "-itu",
        "iterationTimeUnit",
        "Time unit for iteration time"
    ).map { BenchmarkTimeUnit.valueOf(it) }.store()

    val outputTimeUnit by onFlagValue(
        "-otu",
        "outputTimeUnit",
        "Time unit for output values, one of: NANOSECONDS, MICROSECONDS, MILLISECONDS, SECONDS, MINUTES"
    ).map { BenchmarkTimeUnit.valueOf(it) }.store()
    
    val mode by onFlagValue(
        "-m",
        "mode",
        "Result display mode, one of: Throughput, AverageTime"
    ).map { Mode.valueOf(it) }.store()
}

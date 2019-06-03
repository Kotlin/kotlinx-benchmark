package org.jetbrains.gradle.benchmarks

import kotlinx.cli.*

class RunnerConfiguration : CommandLineInterface("Client") {
    val name by onFlagValue("-n", "name", "Name of the configuration").store()
    val filter by onFlagValue("-f", "filter", "Filter for benchmarks, matched by substring").store()
    val reportFile by onFlagValue("-r", "reportFile", "File to save report to").store()
    val traceFormat by onFlagValue("-t", "traceFormat", "Format of tracing report (text or xml)").store("text")
    val iterations by onFlagValue("-i", "iterations", "Number of iterations per benchmark").map { it.toInt() }.store(10)
    val iterationTime by onFlagValue("-ti", "iterationTime", "Time to run one iteration in milliseconds").map { it.toLong() }.store(1000L)
}

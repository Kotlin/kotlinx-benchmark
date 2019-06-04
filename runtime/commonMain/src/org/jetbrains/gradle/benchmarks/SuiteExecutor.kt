package org.jetbrains.gradle.benchmarks

import kotlinx.cli.*

abstract class SuiteExecutor(val executionName: String, arguments: Array<out String>) {
    private val config = RunnerConfiguration().also { it.parse(arguments) }

    val reporter = BenchmarkProgress.create(config.traceFormat)

    private val results = mutableListOf<ReportBenchmarkResult>()

    private val suites = mutableListOf<SuiteDescriptor<*>>()

    fun <T> suite(descriptor: SuiteDescriptor<T>) {
        suites.add(descriptor)
    }

    fun run() {
        //println(config.toString())
        reporter.startSuite(executionName)
        val include = config.include.map { Regex(it) }
        val exclude = config.exclude.map { Regex(it) }

        @Suppress("UNCHECKED_CAST")
        val benchmarks = suites.flatMap { suite ->
            suite.benchmarks
                .filter { benchmark ->
                    val fullName = suite.name + "." + benchmark.name
                    include.any { it.matches(fullName) } && exclude.none { it.matches(fullName) }
                } as List<BenchmarkDescriptor<Any?>>
        }

        run(config, reporter, benchmarks) {
            reporter.endSuite(executionName, results)
            saveReport(config.reportFile, results)
        }
    }

    fun result(result: ReportBenchmarkResult) {
        results.add(result)
    }

    abstract fun run(
        runnerConfiguration: RunnerConfiguration,
        reporter: BenchmarkProgress,
        benchmarks: List<BenchmarkDescriptor<Any?>>,
        complete: () -> Unit
    )
}


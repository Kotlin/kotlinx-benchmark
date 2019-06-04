package org.jetbrains.gradle.benchmarks

import kotlinx.cli.*

abstract class SuiteExecutor(val executionName: String, arguments: Array<out String>) {
    private val config = RunnerConfiguration().also { it.parse(arguments) }

    val reporter = BenchmarkReporter.create(config.reportFile, config.traceFormat)

    private val results = mutableListOf<ReportBenchmarkResult>()

    private val suites = mutableListOf<SuiteDescriptor<*>>()

    fun <T> suite(descriptor: SuiteDescriptor<T>) {
        suites.add(descriptor)
    }

    fun run() {
        reporter.startSuite(executionName)
        val filter = config.filter

        @Suppress("UNCHECKED_CAST")
        val benchmarks = suites.flatMap { suite ->
            suite.benchmarks
                .filter { filter == null || it.name.indexOf(filter) != -1 }
                    as List<BenchmarkDescriptor<Any?>>
        }

        run(config, reporter, benchmarks) {
            reporter.endSuite(executionName, results)
        }
    }

    fun result(result: ReportBenchmarkResult) {
        results.add(result)
    }

    abstract fun run(
        runnerConfiguration: RunnerConfiguration,
        reporter: BenchmarkReporter,
        benchmarks: List<BenchmarkDescriptor<Any?>>,
        complete: () -> Unit
    )
}


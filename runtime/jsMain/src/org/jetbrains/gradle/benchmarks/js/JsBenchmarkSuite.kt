package org.jetbrains.gradle.benchmarks.js

import kotlinx.cli.*
import org.jetbrains.gradle.benchmarks.*
import kotlin.js.*
import kotlin.math.*

external fun require(module: String): dynamic
private val benchmarkJs: dynamic = require("benchmark")
private val process = require("process")

class Suite(private val suiteName: String, @Suppress("UNUSED_PARAMETER") dummy_args: Array<out String>) {
    private val params = RunnerCommandLine().also { it.parse((process["argv"] as Array<String>).drop(2)) }
    private val filter = params.filter
    private val suite: dynamic = benchmarkJs.Suite()
    private val reporter = BenchmarkReporter.create(params.reportFile, params.traceFormat)
    private val results = mutableListOf<ReportBenchmarkResult>()

    fun run() {
        suite.on("complete") {
            reporter.endSuite(suiteName, results)
        }

        reporter.startSuite(suiteName)
        suite.run()
    }

    private fun matchesFilter(name: String): Boolean {
        return filter == null || name.indexOf(filter) != -1
    }

    fun add(name: String, function: () -> Promise<*>, setup: () -> Unit, teardown: () -> Unit) {
        if (!matchesFilter(name))
            return

        suite.add(name) { deferred: Promise<Unit> ->
            // Mind asDynamic: this is **not** a regular promise
            function().then { (deferred.asDynamic()).resolve() }
        }

        configureBenchmark(setup, teardown, asynchronous = true)
    }

    fun add(name: String, function: () -> Any?, setup: () -> Unit, teardown: () -> Unit) {
        if (!matchesFilter(name))
            return

        suite.add(name, function)
        configureBenchmark(setup, teardown, asynchronous = false)
    }

    private fun configureBenchmark(setup: () -> Unit, teardown: () -> Unit, asynchronous: Boolean) {
        val benchmark = suite[suite.length - 1] // take back last added benchmark and subscribe to events

        // TODO: Configure properly
        // initCount: The default number of times to execute a test on a benchmark’s first cycle
        // minTime: The time needed to reduce the percent uncertainty of measurement to 1% (secs).
        // maxTime: The maximum time a benchmark is allowed to run before finishing (secs).

        benchmark.options.minTime = params.iterationTime / 1000.0
        benchmark.options.maxTime = params.iterationTime * params.iterations / 1000.0
        benchmark.options.async = asynchronous
        benchmark.options.defer = asynchronous

        benchmark.on("start") { event ->
            val benchmarkName = event.target.name as String
            reporter.startBenchmark(suiteName, benchmarkName)
            setup()
        }
        benchmark.on("complete") { event ->
            teardown()
            val benchmarkFQN = event.target.name
            val stats = event.target.stats
            val samples = stats.sample.unsafeCast<DoubleArray>().map { 1 / it }.toDoubleArray()
            val result = ReportBenchmarksStatistics.createResult(event.target.name, samples)
            val message = with(result) {
                val d = (4 - log10(score).toInt()).coerceAtLeast(0) // display 4 significant digits
                "  ~ ${score.format(d)} ops/sec ±${(error / score * 100).format(2)}%"
            }
            val error = event.target.error
            if (error == null) {
                reporter.endBenchmark(suiteName, benchmarkFQN, BenchmarkReporter.FinishStatus.Success, message)
                results.add(result)
            } else {
                val stacktrace = error.stack
                reporter.endBenchmarkException(suiteName, benchmarkFQN, error.toString(), stacktrace.toString())
            }
        }
    }
}


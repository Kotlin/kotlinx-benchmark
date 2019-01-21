package org.jetbrains.gradle.benchmarks.js

import org.jetbrains.gradle.benchmarks.*

external fun require(module: String): dynamic
private val benchmarkJs: dynamic = require("benchmark")
private val fs = require("fs");
private val process = require("process");

class Suite(dummy_args: Array<out String>) {
    private val args = (process["argv"] as Array<String>).drop(2)
    private val reportFile = args.first()
    private val suite: dynamic = benchmarkJs.Suite()

    fun run() {
        suite.on("complete") {
            println()
        }
        suite.run()
        fs.writeFile(reportFile, results().toJson()) { err -> if (err) throw err }
    }

    fun add(name: String, function: () -> Any?, setup: () -> Unit, teardown: () -> Unit) {
        suite.add(name, function)
        val benchmark = suite[suite.length - 1] // take back last added benchmark and subscribe to events
        benchmark.on("start") { event ->
            println()
            println("… ${event.target.name}")
            setup()
        }
        benchmark.on("complete") { event ->
            val name = event.target.name
            println("  ${event.target.toString().removePrefix("$name x ")}")
            teardown()
        }

    }

    private fun results(): List<ReportBenchmarkResult> {
        val results = mutableListOf<ReportBenchmarkResult>()
        for (index in 0 until suite.length) {
            val benchmark = suite[index]
            results.add(ReportBenchmarkResult(benchmark.name, benchmark.hz))
        }
        return results
    }
}


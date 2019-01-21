package org.jetbrains.gradle.benchmarks.js

external fun require(module: String): dynamic
private val benchmarkJs : dynamic = require("benchmark")

class Suite {
    private val suite : dynamic = benchmarkJs.Suite()
    
    fun run() {
        suite.run()
        println(resultsJson())
    }

    fun add(name: String, function: () -> Unit) {
        suite.add(name, function)
    }

    private fun resultsJson(): String {
        val list = mutableListOf<BenchmarkResult>()
        for (index in 0 until suite.length) {
            val benchmark = suite[index]
            list.add(BenchmarkResult(benchmark.name, BenchmarkMetric(benchmark.hz)))
        }
        return JSON.stringify(list)
    }
}

data class BenchmarkResult(val benchmark: String, val primaryMetric: BenchmarkMetric)
data class BenchmarkMetric(val score: Double)

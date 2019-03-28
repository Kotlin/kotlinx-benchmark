package org.jetbrains.gradle.benchmarks.jvm

import kotlinx.cli.*
import org.jetbrains.gradle.benchmarks.*
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.*
import org.openjdk.jmh.results.*
import org.openjdk.jmh.runner.*
import org.openjdk.jmh.runner.format.*
import org.openjdk.jmh.runner.options.*
import java.io.*
import java.util.concurrent.*

fun main(args: Array<String>) {
    val params = RunnerCommandLine().also { it.parse(args) }

    val suiteName = params.name ?: run {
        println("Name should be specified")
        return
    }

    // TODO: build options from command line
    val jmhOptions = OptionsBuilder()
        .mode(Mode.Throughput)
        .timeUnit(TimeUnit.SECONDS)
        .warmupIterations(params.iterations)
        .measurementIterations(params.iterations)
        .warmupTime(TimeValue.milliseconds(params.iterationTime))
        .measurementTime(TimeValue.milliseconds(params.iterationTime))
        .forks(1)
        .threads(1)

    val reporter = BenchmarkReporter.create(params.reportFile, params.traceFormat)
    val output = JmhOutputFormat(reporter, suiteName)
    Runner(jmhOptions.build(), output).run()
}

class JmhOutputFormat(private val reporter: BenchmarkReporter, private val suiteName: String) :
    PrintOutputFormat(System.out) {
    override fun startRun() {
        reporter.startSuite(suiteName)
    }

    override fun endRun(result: Collection<RunResult>) {
        reporter.endSuite(suiteName, result.toReportBenchmarkResult())
    }

    override fun startBenchmark(benchParams: BenchmarkParams) {
        val benchmarkFQN = benchParams.benchmark
        reporter.startBenchmark(suiteName, benchmarkFQN)
    }


    override fun endBenchmark(result: BenchmarkResult) {
        val benchmarkFQN = result.params.benchmark
        val value = result.primaryResult
        val message = value.extendedInfo().trim()
        reporter.endBenchmark(suiteName, benchmarkFQN, message)
    }

    override fun iteration(benchParams: BenchmarkParams, params: IterationParams, iteration: Int) {
    }

    override fun iterationResult(
        benchParams: BenchmarkParams,
        params: IterationParams,
        iteration: Int,
        data: IterationResult
    ) {
    }
}

private fun Collection<RunResult>.toReportBenchmarkResult(): Collection<ReportBenchmarkResult> = map { result ->
    val benchmarkFQN = result.params.benchmark
    val value = result.primaryResult

    val (min, max) = value.getScoreConfidence()
    val statistics = value.getStatistics()
    val percentiles = listOf(0.0, 0.25, 0.5, 0.75, 0.90, 0.99, 0.999, 0.9999, 1.0).associate {
        (it * 100) to statistics.getPercentile(it)
    }
    
    val rawData = result.benchmarkResults
        .flatMap { run -> run.iterationResults.map { iteration -> iteration.primaryResult.getScore() } }
        .toDoubleArray()
    
    ReportBenchmarkResult(benchmarkFQN, value.getScore(), value.getScoreError(), min to max, percentiles, rawData)
}

abstract class PrintOutputFormat(private val out: PrintStream) : OutputFormat {
    override fun print(s: String) {
        out.print(s)
    }

    override fun verbosePrintln(s: String) {
    }

    override fun write(b: Int) {
        out.print(b)
    }

    override fun write(b: ByteArray) {
        out.print(b)
    }

    override fun println(s: String) {
        // Don't print other diagnostic messages
        // out.println(s)
    }

    override fun flush() = out.flush()
    override fun close() = flush()
}

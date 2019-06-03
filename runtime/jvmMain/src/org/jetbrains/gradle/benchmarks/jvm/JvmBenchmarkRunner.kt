package org.jetbrains.gradle.benchmarks.jvm

import kotlinx.cli.*
import org.jetbrains.gradle.benchmarks.*
import org.openjdk.jmh.infra.*
import org.openjdk.jmh.results.*
import org.openjdk.jmh.runner.*
import org.openjdk.jmh.runner.format.*
import org.openjdk.jmh.runner.options.*
import java.io.*

fun main(args: Array<String>) {
    val params = RunnerConfiguration().also { it.parse(args) }
    val filter = params.filter

    val suiteName = params.name ?: run {
        println("Name should be specified")
        return
    }

    val jmhOptions = OptionsBuilder()
    if (params.iterations != -1) {
        jmhOptions.warmupIterations(params.iterations)
        jmhOptions.measurementIterations(params.iterations)
    }

    if (params.iterationTime != -1L) {
        jmhOptions.warmupTime(TimeValue.milliseconds(params.iterationTime))
        jmhOptions.measurementTime(TimeValue.milliseconds(params.iterationTime))
    }
    
    filter?.let { jmhOptions.include(it) }

    val reporter = BenchmarkReporter.create(params.reportFile, params.traceFormat)
    val output = JmhOutputFormat(reporter, suiteName)
    try {
        Runner(jmhOptions.build(), output).run()
    } catch (e: Exception) {
        e.printStackTrace()
        reporter.endBenchmark(
            suiteName,
            output.lastBenchmarkStart,
            BenchmarkReporter.FinishStatus.Failure,
            e.message ?: "<unknown error>"
        )
    }
}

class JmhOutputFormat(private val reporter: BenchmarkReporter, private val suiteName: String) :
    PrintOutputFormat(System.out) {
    
    internal var lastBenchmarkStart = ""
    
    override fun startRun() {
        reporter.startSuite(suiteName)
    }

    override fun endRun(result: Collection<RunResult>) {
        reporter.endSuite(suiteName, result.toReportBenchmarkResult())
    }

    override fun startBenchmark(benchParams: BenchmarkParams) {
        val benchmarkFQN = benchParams.benchmark
        reporter.startBenchmark(suiteName, benchmarkFQN)
        lastBenchmarkStart = benchmarkFQN
    }

    override fun endBenchmark(result: BenchmarkResult?) {
        if (result != null) {
            val benchmarkFQN = result.params.benchmark
            val value = result.primaryResult
            val message = value.extendedInfo().trim()
            reporter.endBenchmark(suiteName, benchmarkFQN, BenchmarkReporter.FinishStatus.Success, message)
        } else {
            reporter.endBenchmarkException(suiteName, lastBenchmarkStart, "<ERROR>", "")
        }
    }

    override fun iteration(benchParams: BenchmarkParams, params: IterationParams, iteration: Int) {}

    override fun iterationResult(
        benchParams: BenchmarkParams,
        params: IterationParams,
        iteration: Int,
        data: IterationResult
    ) {
        when (params.type) {
            IterationType.WARMUP -> println("Warm-up $iteration: ${data.primaryResult}")
            IterationType.MEASUREMENT -> println("Iteration $iteration: ${data.primaryResult}")
            null -> throw UnsupportedOperationException("Iteration type not set")
        }
        flush()
    }
    
    override fun println(s: String) {
        if (!s.startsWith("#"))
            reporter.output(suiteName, lastBenchmarkStart, s)
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

abstract class PrintOutputFormat(private val out: PrintStream, private val verbose: VerboseMode = VerboseMode.NORMAL) :
    OutputFormat {

    override fun print(s: String) {
        if (verbose != VerboseMode.SILENT)
            out.print(s)
    }

    override fun verbosePrintln(s: String) {
        if (verbose == VerboseMode.EXTRA)
            out.println(s)
    }

    override fun write(b: Int) {
        if (verbose != VerboseMode.SILENT)
            out.print(b)
    }

    override fun write(b: ByteArray) {
        if (verbose != VerboseMode.SILENT)
            out.print(b)
    }

    override fun println(s: String) {
        if (verbose != VerboseMode.SILENT)
            out.println(s)
    }

    override fun flush() = out.flush()
    override fun close() = flush()
}

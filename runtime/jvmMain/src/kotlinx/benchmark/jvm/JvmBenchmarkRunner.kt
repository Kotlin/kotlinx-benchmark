package kotlinx.benchmark.jvm

import kotlinx.benchmark.*
import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi
import org.openjdk.jmh.infra.*
import org.openjdk.jmh.results.*
import org.openjdk.jmh.results.format.*
import org.openjdk.jmh.runner.*
import org.openjdk.jmh.runner.format.*
import org.openjdk.jmh.runner.options.*
import java.io.*
import java.lang.management.*
import java.util.concurrent.*

@KotlinxBenchmarkRuntimeInternalApi
fun main(args: Array<String>) {
    val config = RunnerConfiguration(args[0].readFile())

    val jmhOptions = OptionsBuilder()
    config.iterations?.let { jmhOptions.measurementIterations(it) }
    config.warmups?.let { jmhOptions.warmupIterations(it) }
    config.iterationTime?.let {
        val timeValue = TimeValue(it, config.iterationTimeUnit ?: TimeUnit.SECONDS)
        jmhOptions.warmupTime(timeValue)
        jmhOptions.measurementTime(timeValue)
    }
    config.outputTimeUnit?.let {
        jmhOptions.timeUnit(it)
    }
    config.mode?.let {
        jmhOptions.mode(it)
    }

    config.include.forEach {
        jmhOptions.include(it)
    }
    config.exclude.forEach {
        jmhOptions.exclude(it)
    }

    config.params.forEach { (key, value) ->
        jmhOptions.param(key, *value.toTypedArray())
    }

    val runtimeMXBean = ManagementFactory.getRuntimeMXBean()
    val jvmArgs = runtimeMXBean.inputArguments
    if (jvmArgs.any { it.contains("libasyncProfiler") }) {
        jmhOptions.forks(0)
    } else {
        when (val jvmForks = config.advanced["jvmForks"]) {
            null -> jmhOptions.forks(1)
            "definedByJmh" -> { /* do not override */ }
            else -> {
                val forks = jvmForks.toIntOrNull()?.takeIf { it >= 0 }
                    ?: throw IllegalArgumentException("jvmForks: expected a non-negative integer or \"definedByJmh\" string literal")
                jmhOptions.forks(forks)
            }
        }
    }

    val reportFormat = ResultFormatType.valueOf(config.reportFormat.uppercase())
    val reporter = BenchmarkProgress.create(config.traceFormat)
    val output = JmhOutputFormat(reporter, config.name)
    try {
        val runner = Runner(jmhOptions.build(), output)
        val results = runner.run()
        val resultFormat = ResultFormatFactory.getInstance(reportFormat, PrintStream(File(config.reportFile)))
        resultFormat.writeOut(results)
    } catch (e: Exception) {
        e.printStackTrace()
        reporter.endBenchmark(
            config.name,
            output.lastBenchmarkStart,
            BenchmarkProgress.FinishStatus.Failure,
            e.message ?: "<unknown error>"
        )
    }
}

@KotlinxBenchmarkRuntimeInternalApi
class JmhOutputFormat(private val reporter: BenchmarkProgress, private val suiteName: String) :
    PrintOutputFormat(System.out) {

    internal var lastBenchmarkStart = ""

    override fun startRun() {
        reporter.startSuite(suiteName)
    }

    override fun endRun(result: Collection<RunResult>) {
        val summary = ByteArrayOutputStream().apply {
            PrintStream(this, true, "UTF-8").use {
                ResultFormatFactory.getInstance(ResultFormatType.TEXT, it).writeOut(result)
            }
        }.toString("UTF-8")
        reporter.endSuite(suiteName, summary)
    }

    override fun startBenchmark(benchParams: BenchmarkParams) {
        val benchmarkId = getBenchmarkId(benchParams)
        reporter.startBenchmark(suiteName, benchmarkId)
        lastBenchmarkStart = benchmarkId
    }

    override fun endBenchmark(result: BenchmarkResult?) {
        if (result != null) {
            val benchmarkId = getBenchmarkId(result.params)
            val value = result.primaryResult
            val message = value.extendedInfo().trim()
            reporter.endBenchmark(suiteName, benchmarkId, BenchmarkProgress.FinishStatus.Success, message)
        } else {
            reporter.endBenchmarkException(suiteName, lastBenchmarkStart, "<ERROR>", "")
        }
    }

    private fun getBenchmarkId(params: BenchmarkParams): String {
        val benchmarkName = params.benchmark
        val paramKeys = params.paramsKeys
        val benchmarkId = if (paramKeys.isEmpty())
            benchmarkName
        else
            benchmarkName + paramKeys.joinToString(prefix = " | ") { "$it=${params.getParam(it)}" }
        return benchmarkId
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

/*
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
*/

@KotlinxBenchmarkRuntimeInternalApi
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

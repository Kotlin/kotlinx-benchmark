package org.jetbrains.gradle.benchmarks.jvm

import org.jetbrains.gradle.benchmarks.*
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.*
import org.openjdk.jmh.results.*
import org.openjdk.jmh.results.format.*
import org.openjdk.jmh.runner.*
import org.openjdk.jmh.runner.format.*
import org.openjdk.jmh.runner.options.*
import java.io.*
import java.util.concurrent.*

fun main(args: Array<String>) {
    val reportFile = args.firstOrNull()
    if (reportFile == null) {
        println("Invalid invocation, should provide report file path")
        return
    }

    val iterations = args[1].toInt()
    val iterationTime = args[2].toLong()
    val format = args[3]
    val title = args[4]

    // TODO: build options from command line
    val jmhOptions = OptionsBuilder()
        .mode(Mode.Throughput)
        .timeUnit(TimeUnit.SECONDS)
        .warmupIterations(iterations)
        .measurementIterations(iterations)
        .warmupTime(TimeValue.milliseconds(iterationTime))
        .measurementTime(TimeValue.milliseconds(iterationTime))
        .forks(1)

    val options = jmhOptions.apply {
        threads(1)
    }
    val output = JmhOutputFormat(reportFile, format, title)

    when (format) {
        "xml" -> {
            println(ijLogStart(title, ""))
        }
        "text" -> {}
        else -> throw UnsupportedOperationException("Format $format is not supported.")
    }

    Runner(options.build(), output).run()

    when (format) {
        "xml" -> {
            println(ijLogFinish(title, ""))
        }
        "text" -> {}
        else -> throw UnsupportedOperationException("Format $format is not supported.")
    }

}

abstract class PrintOutputFormat(val out: PrintStream) : OutputFormat {

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

    fun printMessage(s: String) = out.print(s)
    fun printMessageLine(s: String) = out.println(s)

    override fun flush() = out.flush()
    override fun close() = flush()
}

class JmhOutputFormat(val reportFile: String, val format: String, val title: String) : PrintOutputFormat(System.out) {
    override fun startRun() {
    }

    override fun endRun(result: Collection<RunResult>) {
        printMessageLine("")
        File(reportFile).outputStream().use {
            val reportFormat = ResultFormatFactory.getInstance(ResultFormatType.JSON, PrintStream(it))
            reportFormat.writeOut(result)
        }
    }

    override fun startBenchmark(benchParams: BenchmarkParams) {
        val benchmarkFQN = benchParams.benchmark
        when (format) {
            "xml" -> {
                printMessageLine(ijLogStart(benchmarkFQN, title))
            }
            "text" -> {
                printMessageLine("")
                printMessageLine("… $benchmarkFQN")
            }
            else -> throw UnsupportedOperationException("Format $format is not supported.")
        }
    }


    override fun endBenchmark(result: BenchmarkResult) {
        val benchmarkFQN = result.params.benchmark
        val value = result.primaryResult
        val message = value.extendedInfo().trim()
        when (format) {
            "xml" -> {
                printMessageLine(ijLogOutput(benchmarkFQN, title, message))
                printMessageLine(ijLogFinish(benchmarkFQN, title))
            }
            "text" -> {
                printMessageLine("  $message")
            }
            else -> throw UnsupportedOperationException("Format $format is not supported.")
        }
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

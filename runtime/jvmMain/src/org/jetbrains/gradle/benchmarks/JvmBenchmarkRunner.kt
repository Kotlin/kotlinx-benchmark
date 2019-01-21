package org.jetbrains.gradle.benchmarks

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

    // TODO: build options from command line
    val jmhOptions = OptionsBuilder()
        .mode(Mode.Throughput)
        .timeUnit(TimeUnit.SECONDS)
        .warmupIterations(5)
        .measurementIterations(5)
        .warmupTime(TimeValue.milliseconds(500))
        .measurementTime(TimeValue.milliseconds(500))
        .forks(1)

    val options = jmhOptions.apply {
        threads(1)
    }
    val output = JmhOutputFormat(reportFile)
    Runner(options.build(), output).run()
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

class JmhOutputFormat(val reportFile: String) : PrintOutputFormat(System.out) {
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
        printMessageLine("")
        printMessageLine("… ${benchParams.benchmark}")
    }

    override fun endBenchmark(result: BenchmarkResult) {
        val value = result.primaryResult
        printMessageLine("  ${value.extendedInfo().trim()}")
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

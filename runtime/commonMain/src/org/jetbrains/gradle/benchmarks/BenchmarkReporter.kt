package org.jetbrains.gradle.benchmarks;

abstract class BenchmarkReporter(val reportFile: String?) {
    abstract fun startSuite(suite: String)
    abstract fun endSuite(suite: String, result: Collection<ReportBenchmarkResult>)

    abstract fun startBenchmark(suite: String, benchmark: String)
    abstract fun endBenchmark(suite: String, benchmark: String, status: FinishStatus, message: String)
    abstract fun endBenchmarkException(suite: String, benchmark: String, error: String, stacktrace: String)
    abstract fun output(suite: String, benchmark: String, message: String)

    companion object {
        fun create(reportFile: String?, format: String) = when (format) {
            "xml" -> {
                IntelliJBenchmarkReporter(reportFile)
            }
            "text" -> {
                ConsoleBenchmarkReporter(reportFile)
            }
            else -> throw UnsupportedOperationException("Format $format is not supported.")
        }
    }

    enum class FinishStatus {
        Success,
        Failure
    }
}

class IntelliJBenchmarkReporter(reportFile: String?) : BenchmarkReporter(reportFile) {
    private val rootId = "[root]"

    override fun startSuite(suite: String) {
        currentStatus = FinishStatus.Success
        println(ijSuiteStart("", rootId))
        println(ijSuiteStart(rootId, suite))
    }

    override fun endSuite(suite: String, result: Collection<ReportBenchmarkResult>) {
        if (currentClass != "") {
            println(ijSuiteFinish(suite, currentClass, currentStatus))
        }
        println(ijSuiteFinish(rootId, suite, suiteStatus))
        println(ijSuiteFinish("", rootId, suiteStatus))
        saveReport(reportFile, result)
    }

    private var currentClass = ""
    private var currentStatus = FinishStatus.Success
    private var suiteStatus = FinishStatus.Success

    override fun startBenchmark(suite: String, benchmark: String) {
        val methodName = benchmark.substringAfterLast('.')
        val className = benchmark.substringBeforeLast('.')
        if (currentClass != className) {
            if (currentClass != "") {
                println(ijSuiteFinish(suite, currentClass, currentStatus))
            }
            currentStatus = FinishStatus.Success
            println(ijSuiteStart(suite, className))
            currentClass = className
        }

        println(ijBenchmarkStart(currentClass, className, methodName))
        println(ijLogOutput(currentClass, benchmark, "$suite: $benchmark\n"))
    }

    override fun endBenchmark(suite: String, benchmark: String, status: FinishStatus, message: String) {
        println(ijLogOutput(currentClass, benchmark, "$message\n\n"))
        println(ijBenchmarkFinish(currentClass, benchmark, status))
    }

    override fun endBenchmarkException(suite: String, benchmark: String, error: String, stacktrace: String) {
        currentStatus = FinishStatus.Failure
        suiteStatus = FinishStatus.Failure
        println(ijBenchmarkFinishException(currentClass, benchmark, error, stacktrace))
    }

    override fun output(suite: String, benchmark: String, message: String) {
        println(ijLogOutput(currentClass, benchmark, "$message\n"))
    }
}

class ConsoleBenchmarkReporter(reportFile: String?) : BenchmarkReporter(reportFile) {
    override fun startSuite(suite: String) {

    }

    override fun endSuite(suite: String, result: Collection<ReportBenchmarkResult>) {
        println()
        saveReport(reportFile, result)
    }

    override fun startBenchmark(suite: String, benchmark: String) {
        println()
        println("… $benchmark")

    }

    override fun endBenchmark(suite: String, benchmark: String, status: FinishStatus, message: String) {
        println("  $status: $message")
    }

    override fun endBenchmarkException(suite: String, benchmark: String, error: String, stacktrace: String) {
        println("  EXCEPTION: $error")
        println(stacktrace)
    }

    override fun output(suite: String, benchmark: String, message: String) {
        println(message)
    }
}

expect fun saveReport(reportFile: String?, results: Collection<ReportBenchmarkResult>)
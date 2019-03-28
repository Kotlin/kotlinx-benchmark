package org.jetbrains.gradle.benchmarks;

abstract class BenchmarkReporter(val reportFile: String?) {
    abstract fun startSuite(suite: String)
    abstract fun endSuite(suite: String, result: Collection<ReportBenchmarkResult>)

    abstract fun startBenchmark(suite: String, benchmark: String)
    abstract fun endBenchmark(suite: String, benchmark: String, message: String)

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
}

class IntelliJBenchmarkReporter(reportFile: String?) : BenchmarkReporter(reportFile) {
    override fun startSuite(suite: String) {
        println(ijSuiteStart("", suite))
    }

    override fun endSuite(suite: String, result: Collection<ReportBenchmarkResult>) {
        if (currentClass != "") {
            println(ijLogFinish(suite, currentClass))
        }
        println(ijLogFinish("", suite))
        saveReport(reportFile, result)
    }

    private var currentClass = ""
    
    override fun startBenchmark(suite: String, benchmark: String) {
        val methodName = benchmark.substringAfterLast('.')
        val className = benchmark.substringBeforeLast('.')
        if (currentClass != className) {
            if (currentClass != "") {
                println(ijLogFinish(suite, currentClass))
            }
            println(ijSuiteStart(suite, className))
            currentClass = className
        }

        println(ijBenchmarkStart(currentClass, className, methodName))
        println(ijLogOutput(currentClass, benchmark, "$suite: $benchmark\n"))
    }

    override fun endBenchmark(suite: String, benchmark: String, message: String) {
        println(ijLogOutput(currentClass, benchmark, "$message\n\n"))
        println(ijLogFinish(currentClass, benchmark))
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

    override fun endBenchmark(suite: String, benchmark: String, message: String) {
        println("  $message")
    }
}

expect fun saveReport(reportFile: String?, results: Collection<ReportBenchmarkResult>)
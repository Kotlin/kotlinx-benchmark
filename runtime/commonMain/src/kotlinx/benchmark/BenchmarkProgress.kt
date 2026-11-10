package kotlinx.benchmark

import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi

@KotlinxBenchmarkRuntimeInternalApi
abstract class BenchmarkProgress {
    abstract fun startSuite(suite: String)
    abstract fun endSuite(suite: String, summary: String)

    abstract fun startBenchmark(suite: String, benchmark: String)
    abstract fun endBenchmark(suite: String, benchmark: String, status: FinishStatus, message: String)
    abstract fun endBenchmarkException(suite: String, benchmark: String, error: String, stacktrace: String)
    abstract fun output(suite: String, benchmark: String, message: String)

    @KotlinxBenchmarkRuntimeInternalApi
    companion object {
        fun create(format: String, xml: (() -> BenchmarkProgress)? = null) = when (format) {
            "xml" -> {
                xml?.invoke() ?: IntelliJBenchmarkProgress()
            }
            "text" -> {
                ConsoleBenchmarkProgress()
            }
            else -> throw UnsupportedOperationException("Format $format is not supported.")
        }
    }

    @KotlinxBenchmarkRuntimeInternalApi
    enum class FinishStatus {
        Success,
        Failure
    }
}

@KotlinxBenchmarkRuntimeInternalApi
open class IntelliJBenchmarkProgress : BenchmarkProgress() {
    private val rootId = "[root]"

    override fun startSuite(suite: String) {
        currentStatus = FinishStatus.Success
        println(ijSuiteStart("", rootId))
        println(ijSuiteStart(rootId, suite))
    }

    override fun endSuite(suite: String, summary: String) {
        if (currentClass != "") {
            println(ijSuiteFinish(suite, currentClass, currentStatus))
        }
        println(ijLogOutput(rootId, suite, "$suite summary:\n$summary\n"))
        println(ijSuiteFinish(rootId, suite, suiteStatus))
        println(ijSuiteFinish("", rootId, suiteStatus))
    }

    protected var currentClass = ""
    protected var currentStatus = FinishStatus.Success
    protected var suiteStatus = FinishStatus.Success

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

@KotlinxBenchmarkRuntimeInternalApi
class ConsoleBenchmarkProgress : BenchmarkProgress() {
    override fun startSuite(suite: String) {

    }

    override fun endSuite(suite: String, summary: String) {
        println()
        println("$suite summary:")
        println(summary)
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


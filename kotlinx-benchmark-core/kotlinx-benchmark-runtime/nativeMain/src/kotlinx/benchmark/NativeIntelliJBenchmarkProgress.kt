package kotlinx.benchmark

import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi

// This implementation is used in Kotlin/Native target, where each benchmark is run in a separate process.
// Previous benchmark id is stored to identify when previous suite (benchmark class) ends.
@KotlinxBenchmarkRuntimeInternalApi
class NativeIntelliJBenchmarkProgress(
    private val benchProgressPath: String
) : IntelliJBenchmarkProgress() {

    init {
        decode()
    }

    override fun startBenchmark(suite: String, benchmark: String) {
        super.startBenchmark(suite, benchmark)
        encode()
    }

    override fun endBenchmarkException(suite: String, benchmark: String, error: String, stacktrace: String) {
        super.endBenchmarkException(suite, benchmark, error, stacktrace)
        encode()
    }

    private fun encode() {
        benchProgressPath.writeFile(listOf(currentClass, currentStatus, suiteStatus).joinToString(separator = "\n"))
    }

    private fun decode() {
        val content = benchProgressPath.readFile()
        if (content.isEmpty()) {
            return
        }

        val (currentClass, currentStatus, suiteStatus) = content.lines()
        this.currentClass = currentClass
        this.currentStatus = FinishStatus.valueOf(currentStatus)
        this.suiteStatus = FinishStatus.valueOf(suiteStatus)
    }
}
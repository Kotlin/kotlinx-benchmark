package kotlinx.benchmark

import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi

@KotlinxBenchmarkRuntimeInternalApi
abstract class Measurer {
    abstract fun measureStart()
    abstract fun measureFinish(): Long
}

@KotlinxBenchmarkRuntimeInternalApi
abstract class BenchmarkEngineSupport {
    abstract fun readFile(path: String): String
    abstract fun writeFile(path: String, content: String)
    abstract fun arguments(): Array<out String>
    abstract fun getMeasurer(): Measurer
    abstract fun isSupported(): Boolean
}

@KotlinxBenchmarkRuntimeInternalApi
fun overrideEngineSupport(overriddenSupport: BenchmarkEngineSupport) {
    check(overriddenSupport.isSupported()) { "Engine is not supported" }
    engineSupport = overriddenSupport
}

internal expect var engineSupport: BenchmarkEngineSupport

internal actual fun String.readFile(): String =
    engineSupport.readFile(this)

internal actual fun String.writeFile(text: String): Unit =
    engineSupport.writeFile(this, text)
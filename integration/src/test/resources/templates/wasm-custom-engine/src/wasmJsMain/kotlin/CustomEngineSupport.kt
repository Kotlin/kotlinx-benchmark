package test

import kotlinx.benchmark.BenchmarkEngineSupport
import kotlinx.benchmark.Measurer
import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi
import kotlinx.benchmark.overrideEngineSupport
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@JsFun("""
    (globalThis.module = (typeof process !== 'undefined') && (process.release.name === 'node') ?
        await import(/* webpackIgnore: true */'node:module') : void 0, () => {})
""")
private external fun persistModule()

private fun getRequire(): JsAny =
    js("""{ 
    const importMeta = import.meta;
    return globalThis.module.default.createRequire(importMeta.url);
}""")

private fun nodeJsReadFile(require: JsAny, path: String): String =
    js("require('fs').readFileSync(path, 'utf8')")

private fun nodeJsArguments(): String =
    js("process.argv.slice(2).join(' ')")

@OptIn(KotlinxBenchmarkRuntimeInternalApi::class)
private object FakeEngineSupport : BenchmarkEngineSupport() {
    private val require by lazy { persistModule().let { getRequire() } }

    override fun writeFile(path: String, content: String) {
        println("Custom engine write to file")
        println("<FILE:$path>$content<ENDFILE>")
    }

    override fun readFile(path: String): String {
        println("Custom engine read from file")
        return nodeJsReadFile(require, path)
    }

    override fun arguments(): Array<out String> =
        nodeJsArguments().split(' ').toTypedArray()

    override fun getMeasurer(): Measurer = FakeMeasurer

    override fun isSupported(): Boolean = true
}

@OptIn(KotlinxBenchmarkRuntimeInternalApi::class)
val FakeMeasurer = object : Measurer() {
    private var measureStartCalled = false
    private var measureFinishCalled = false
    private var start: Long = 0L
    override fun measureStart() {
        if (!measureStartCalled) {
            println("Custom engine measurer start")
            measureStartCalled = true
        }
        start = 10000L
    }
    override fun measureFinish(): Long {
        if (!measureFinishCalled) {
            println("Custom engine measurer finish")
            measureFinishCalled = true
        }
        return start + 20000L
    }
}

@Suppress("DEPRECATION")
@OptIn(ExperimentalStdlibApi::class, KotlinxBenchmarkRuntimeInternalApi::class)
@EagerInitialization
private val initializeCustomEngines: Unit = run {
    println("Custom engine registered")
    overrideEngineSupport(FakeEngineSupport)
}

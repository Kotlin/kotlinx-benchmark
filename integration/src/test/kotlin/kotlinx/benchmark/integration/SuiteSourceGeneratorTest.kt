package kotlinx.benchmark.integration

import java.util.*
import kotlin.test.*

class SuiteSourceGeneratorTest : GradleTest() {
    @Test
    fun measurementAnnotation() {
        project("kotlin-multiplatform", true).let { runner ->
            runner.updateAnnotations("src/commonMain/kotlin/CommonBenchmark.kt") {
                measurement(iterations = 5, time = 200, timeUnit = "BenchmarkTimeUnit.MILLISECONDS")
            }

            runner.run("nativeBenchmarkGenerate")

            runner.generatedDir("native", "test/CommonBenchmark_Descriptor.kt") { descriptorFile ->
                val text = descriptorFile.readText()
                val parameters = "iterations = 5, iterationTime = IterationTime\\(200, BenchmarkTimeUnit.MILLISECONDS\\)"
                    .replace(" ", "\\s+").toRegex()
                assertTrue(text.contains(parameters), "Parameters: <$parameters> not found in <$text>")
            }
        }
    }

    @Test
    fun generateAndCompileNativeBenchmarks() {
        generateAndCompile("native")
    }

    @Test
    fun generateAndCompileJSBenchmarks() {
        generateAndCompile("jsIr")
    }

    @Test
    fun generateAndCompileWasmBenchmarks() {
        generateAndCompile("wasmJs")
    }

    private fun generateAndCompile(target: String) {
        project("kotlin-multiplatform", true).let { runner ->
            runner.run(":${target}BenchmarkGenerate")

            runner.generatedDir(target, "RootBenchmark_Descriptor.kt") { descriptorFile ->
                val text = descriptorFile.readText()
                assertFalse(
                    text.contains("<root>"),
                    "Generated descriptor contains illegal characters '<root>' in $text"
                )
            }

            val capitalizedTarget = target.replaceFirstChar { it.uppercaseChar() }
            runner.run(":compile${capitalizedTarget}BenchmarkKotlin${capitalizedTarget}")
        }
    }
}
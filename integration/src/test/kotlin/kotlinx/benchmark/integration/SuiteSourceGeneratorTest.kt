package kotlinx.benchmark.integration

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
    fun outputTimeUnitAnnotation() {
        project("kotlin-multiplatform", true).let { runner ->
            runner.updateAnnotations("src/commonMain/kotlin/CommonBenchmark.kt") {
                outputTimeUnit("BenchmarkTimeUnit.SECONDS")
            }

            runner.run("nativeBenchmarkGenerate")

            runner.generatedDir("native", "test/CommonBenchmark_Descriptor.kt") { descriptorFile ->
                val text = descriptorFile.readText()
                val parameters = "outputTimeUnit = BenchmarkTimeUnit.SECONDS"
                    .replace(" ", "\\s+").toRegex()
                assertTrue(text.contains(parameters), "Parameters: <$parameters> not found in <$text>")
            }
        }
    }    
}
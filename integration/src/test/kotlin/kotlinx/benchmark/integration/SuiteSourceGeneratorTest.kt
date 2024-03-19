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
    
    @Test
    fun benchmarkModeAnnotation() {
        project("kotlin-multiplatform", true).let { runner ->
            runner.updateAnnotations("src/commonMain/kotlin/CommonBenchmark.kt") {
                benchmarkMode("Mode.AverageTime")
            }

            runner.run("nativeBenchmarkGenerate")

            runner.generatedDir("native", "test/CommonBenchmark_Descriptor.kt") { descriptorFile ->
                val text = descriptorFile.readText()
                val parameters = "mode = Mode.AverageTime"
                    .replace(" ", "\\s+").toRegex()
                assertTrue(text.contains(parameters), "Parameters: <$parameters> not found in <$text>")
            }
        }
    }

    @Test
    fun setupAnnotation() {
        project("kotlin-multiplatform", true).let { runner ->
            runner.addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
                setup("setUpMethod")
            }

            runner.run("nativeBenchmarkGenerate")

            runner.generatedDir("native", "test/CommonBenchmark_Descriptor.kt") { descriptorFile ->
                val text = descriptorFile.readText()
                val expectedFunctionCall = "instance.setUpMethod()"
                assertTrue(text.contains(expectedFunctionCall), "Function call <$expectedFunctionCall> not found in <$text>")
            }
        }
    }

    @Test
    fun teardownAnnotation() {
        project("kotlin-multiplatform", true).let { runner ->
            runner.addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
                teardown("teardownMethod")
            }

            runner.run("nativeBenchmarkGenerate")

            runner.generatedDir("native", "test/CommonBenchmark_Descriptor.kt") { descriptorFile ->
                val text = descriptorFile.readText()
                val expectedFunctionCall = "instance.teardownMethod()"
                assertTrue(text.contains(expectedFunctionCall), "Function call <$expectedFunctionCall> not found in <$text>")
            }
        }
    }
    
    @Test
    fun paramFieldAnnotation() {
        project("kotlin-multiplatform", true).let { runner ->
            runner.addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
                param("data", "1", "2")
            }
    
            runner.run("nativeBenchmarkGenerate")
    
            runner.generatedDir("native", "test/CommonBenchmark_Descriptor.kt") { descriptorFile ->
                val text = descriptorFile.readText()
                val expectedAnnotation = "defaultParameters = mapOf(\"data\" to listOf(\"\"\"1\"\"\", \"\"\"2\"\"\"))"
                assertTrue(text.contains(expectedAnnotation), "Annotation <$expectedAnnotation> not found in <$text>")
            }
        }
    }
}
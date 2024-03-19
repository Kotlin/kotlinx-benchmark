package kotlinx.benchmark.integration

import kotlin.test.*

class SuiteSourceGeneratorTest : GradleTest() {

    private fun Runner.assertGeneratedDescriptorContains(substring: String) {
        generatedDir("native", "test/CommonBenchmark_Descriptor.kt") { descriptorFile ->
            val text = descriptorFile.readText()
            assertTrue(text.contains(substring), "Substring: <$substring> not found in <$text>")
        }
    }

    private fun Runner.assertGeneratedDescriptorContains(pattern: Regex) {
        generatedDir("native", "test/CommonBenchmark_Descriptor.kt") { descriptorFile ->
            val text = descriptorFile.readText()
            assertTrue(text.contains(pattern), "Pattern: <$pattern> not found in <$text>")
        }
    }

    private inline fun testSourceGenerator(setupBlock: Runner.() -> Unit, checkBlock: Runner.() -> Unit) {
        project("kotlin-multiplatform", true).apply {
            setupBlock()
            run("nativeBenchmarkGenerate")
            checkBlock()
        }
    }

    @Test
    fun measurementAnnotation() {
        testSourceGenerator(
            setupBlock = {
                updateAnnotations("src/commonMain/kotlin/CommonBenchmark.kt") {
                    measurement(iterations = 5, time = 200, timeUnit = "BenchmarkTimeUnit.MILLISECONDS")
                }
            },
            checkBlock = {
                val parameters = "iterations = 5, iterationTime = IterationTime\\(200, BenchmarkTimeUnit.MILLISECONDS\\)"
                    .replace(" ", "\\s+").toRegex()
                assertGeneratedDescriptorContains(parameters)
            }
        )
    }

    @Test
    fun outputTimeUnitAnnotation() {
        testSourceGenerator(
            setupBlock = {
                updateAnnotations("src/commonMain/kotlin/CommonBenchmark.kt") {
                    outputTimeUnit("BenchmarkTimeUnit.SECONDS")
                }
            },
            checkBlock = {
                val parameters = "outputTimeUnit = BenchmarkTimeUnit.SECONDS"
                    .replace(" ", "\\s+").toRegex()
                assertGeneratedDescriptorContains(parameters)
            }
        )
    }

    @Test
    fun benchmarkModeAnnotation() {
        testSourceGenerator(
            setupBlock = {
                updateAnnotations("src/commonMain/kotlin/CommonBenchmark.kt") {
                    benchmarkMode("Mode.AverageTime")
                }
            },
            checkBlock = {
                val parameters = "mode = Mode.AverageTime"
                    .replace(" ", "\\s+").toRegex()
                assertGeneratedDescriptorContains(parameters)
            }
        )
    }

    @Test
    fun setupAnnotation() {
        testSourceGenerator(
            setupBlock = {
                addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
                    setup("setUpMethod")
                }
            },
            checkBlock = {
                val expectedFunctionCall = "instance.setUpMethod()"
                assertGeneratedDescriptorContains(expectedFunctionCall)
            }
        )
    }

    @Test
    fun teardownAnnotation() {
        testSourceGenerator(
            setupBlock = {
                addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
                    teardown("teardownMethod")
                }
            },
            checkBlock = {
                val expectedFunctionCall = "instance.teardownMethod()"
                assertGeneratedDescriptorContains(expectedFunctionCall)
            }
        )
    }

    @Test
    fun paramFieldAnnotation() {
        testSourceGenerator(
            setupBlock = {
                addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
                    param("data", "1", "2")
                }
            },
            checkBlock = {
                val expectedParameterAnnotation = "parameters = listOf(\"data\")"
                val expectedDefaultParametersAnnotation = "defaultParameters = mapOf(\"data\" to listOf(\"\"\"1\"\"\", \"\"\"2\"\"\"))"
                assertGeneratedDescriptorContains(expectedParameterAnnotation)
                assertGeneratedDescriptorContains(expectedDefaultParametersAnnotation)
            }
        )
    }
}
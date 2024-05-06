package kotlinx.benchmark.integration

import kotlin.test.*

class SuiteSourceGeneratorTest : GradleTest() {
    private fun Runner.assertGeneratedDescriptorContainsCode(code: String) {
        val regex = code.replace(" ", "\\s*").toRegex(RegexOption.DOT_MATCHES_ALL)
        generatedDir("native", "test/CommonBenchmark_Descriptor.kt") { descriptorFile ->
            val text = descriptorFile.readText()
            assertTrue(text.contains(regex), "Regex: <$regex> not found in <$text>")
        }
    }

    private inline fun testSourceGenerator(setupBlock: Runner.() -> Unit, checkBlock: Runner.() -> Unit) {
        project("source-generation").apply {
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
                    measurement(iterations = 12, time = 200, timeUnit = "BenchmarkTimeUnit.MILLISECONDS")
                }
            },
            checkBlock = {
                val parameters = "iterations = 12, warmups = 5, iterationTime = IterationTime\\(200, BenchmarkTimeUnit.MILLISECONDS\\)"
                assertGeneratedDescriptorContainsCode(parameters)
            }
        )
    }

    @Test
    fun warmupAnnotation() {
        testSourceGenerator(
            setupBlock = {
                updateAnnotations("src/commonMain/kotlin/CommonBenchmark.kt") {
                    warmup(iterations = 12, time = 200, timeUnit = "BenchmarkTimeUnit.MILLISECONDS")
                }
            },
            checkBlock = {
                // time and timeUnit of @Warmup are ignored: https://github.com/Kotlin/kotlinx-benchmark/issues/74
                val parameters = "iterations = 3, warmups = 12, iterationTime = IterationTime\\(1, BenchmarkTimeUnit.SECONDS\\)"
                assertGeneratedDescriptorContainsCode(parameters)
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
                assertGeneratedDescriptorContainsCode(parameters)
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
                assertGeneratedDescriptorContainsCode(parameters)
            }
        )
    }

    @Test
    fun setupAnnotation() {
        testSourceGenerator(
            setupBlock = {
                addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
                    setup("function1")
                    setup("function2")
                }
            },
            checkBlock = {
                val functionCalls = """((instance.function1\(\).*instance.function2\(\))|(instance.function2\(\).*instance.function1\(\)))"""
                val regex = "private fun setUp\\(instance: CommonBenchmark\\) \\{ $functionCalls }"
                assertGeneratedDescriptorContainsCode(regex)
            }
        )
    }

    @Test
    fun benchmarkAnnotation() {
        testSourceGenerator(
            setupBlock = {
                addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
                    benchmark("function1")
                    benchmark("function2")
                }
            },
            checkBlock = {
                val function1Ref = """CommonBenchmark::function1"""
                val function2Ref = """CommonBenchmark::function2"""
                assertGeneratedDescriptorContainsCode(function1Ref)
                assertGeneratedDescriptorContainsCode(function2Ref)
            }
        )
    }

    @Test
    fun teardownAnnotation() {
        testSourceGenerator(
            setupBlock = {
                addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
                    teardown("function1")
                    teardown("function2")
                }
            },
            checkBlock = {
                val functionCalls = """((instance.function1\(\).*instance.function2\(\))|(instance.function2\(\).*instance.function1\(\)))"""
                val regex = "private fun tearDown\\(instance: CommonBenchmark\\) \\{ $functionCalls } "
                assertGeneratedDescriptorContainsCode(regex)
            }
        )
    }

    @Test
    fun paramFieldAnnotation() {
        testSourceGenerator(
            setupBlock = {
                addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
                    param("data1", "1", "2")
                    param("data2", "a", "b")
                }
            },
            checkBlock = {
                val parameterList = "parameters = listOf\\(\"data1\", \"data2\"\\)"
                val data1Values = "\"data1\" to listOf\\(\"\"\"1\"\"\", \"\"\"2\"\"\"\\)"
                val data2Values = "\"data2\" to listOf\\(\"\"\"a\"\"\", \"\"\"b\"\"\"\\)"
                assertGeneratedDescriptorContainsCode(parameterList)
                assertGeneratedDescriptorContainsCode(data1Values)
                assertGeneratedDescriptorContainsCode(data2Values)
            }
        )
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
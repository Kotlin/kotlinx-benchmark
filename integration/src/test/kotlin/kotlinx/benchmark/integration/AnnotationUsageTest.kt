package kotlinx.benchmark.integration

import kotlin.test.*

class AnnotationUsageTest : GradleTest() {

    val platforms = listOf("nativeBenchmark", "wasmJsBenchmark", "jsIrBenchmark")

    private fun executeBenchmark(
        annotations: List<Pair<String, String>>,
        error: String = "",
        jvmSpecificError: String = ""
    ) {
        val runner = project("kotlin-multiplatform")
        configureBenchmarkAnnotations(runner, annotations)

        val hasError = error.isNotEmpty()

        if (hasError) {
            runBenchmarkWithErrors(runner, error)
        } else {
            runBenchmarkSuccessfully(runner)
        }

        runJvmBenchmark(runner, jvmSpecificError)
    }

    private fun configureBenchmarkAnnotations(runner: Runner, annotations: List<Pair<String, String>>) {
        runner.addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
            annotations.forEach { (annotation, method) ->
                when (annotation) {
                    "benchmark" -> benchmark(method)
                    "setup" -> setup(method)
                    "teardown" -> teardown(method)
                    "param" -> {
                        val splitMethod = method.split(",")
                        if (splitMethod.isNotEmpty()) {
                            val fieldName = splitMethod.first()
                            val values = splitMethod.drop(1).toTypedArray()
                            param(fieldName, *values)
                        }
                    }
                }
            }
        }
    }

    private fun runBenchmarkSuccessfully(runner: Runner) {
        platforms.forEach { platform ->
            runner.run(platform) {
                assertOutputContains("BUILD SUCCESSFUL")
            }
        }
    }

    private fun runBenchmarkWithErrors(runner: Runner, error: String) {
        platforms.forEach { platform ->
            runner.runAndFail(platform) {
                assertOutputContains(error)
            }
        }
    }

    private fun runJvmBenchmark(runner: Runner, jvmSpecificError: String) {
        if (jvmSpecificError.isNotEmpty()) {
            runner.runAndFail("jvmBenchmark") {
                assertOutputContains(jvmSpecificError)
            }
        } else {
            runner.run("jvmBenchmark") {
                assertOutputContains("BUILD SUCCESSFUL")
            }
        }
    }


    @Test
    fun testParamWithVal() {
        executeBenchmark(
            annotations = listOf("param" to "valProperty,1,5"),
            error = "Invalid usage of @Param: Property `valProperty` is read-only (val). Ensure properties annotated with @Param are mutable (var).",
            jvmSpecificError = "@Param annotation is not acceptable on final fields."
        )
    }

    @Test
    fun testParamWithPrivate() {
        executeBenchmark(
            annotations = listOf("param" to "privateProperty,1,5"),
            error = "Invalid usage of @Param: Property `privateProperty` is private. Ensure properties annotated with @Param are not declared private.",
        )
    }

    @Test
    fun testParamWithFinal() {
        executeBenchmark(
            annotations = listOf("param" to "finalProperty,1,5")
        )
    }

    @Test
    fun testParamWithInternal() {
        executeBenchmark(
            annotations = listOf("param" to "internalProperty,1,5"),
            error = "Invalid usage of @Param: Property `internalProperty` is internal. Ensure properties annotated with @Param are not declared internal.",
        )
    }

    @Test
    fun testParamWithMethod() {
        executeBenchmark(
            annotations = listOf("param" to "plainMethod")
        )
    }


    @Test
    fun testBenchmarkWithFinalMethod() {
        executeBenchmark(
            annotations = listOf("benchmark" to "finalMethod")
        )
    }


    @Test
    fun testBenchmarkWithPrivate() {
        executeBenchmark(
            annotations = listOf("benchmark" to "privateMethod"),
            error = "Invalid usage of @Benchmark: Method `privateMethod` is private. @Benchmark methods should be public.",
            jvmSpecificError = "@Benchmark method should be public."
        )
    }

    @Test
    fun testBenchmarkWithInternal() {
        executeBenchmark(
            annotations = listOf("benchmark" to "internalMethod"),
            error = "Invalid usage of @Benchmark: Method `internalMethod` is internal."
        )
    }


    @Test
    fun testSetupWithPrivate() {
        executeBenchmark(
            annotations = listOf("setup" to "privateMethod"),
            error = "Invalid usage of @Setup: Method `privateMethod` is private. @Setup methods should be public.",
            jvmSpecificError = "@Setup method should be public."
        )
    }

    @Test
    fun testSetupWithFinal() {
        executeBenchmark(
            annotations = listOf("setup" to "finalMethod")
        )
    }

    @Test
    fun testSetupWithInternal() {
        executeBenchmark(
            annotations = listOf("setup" to "internalMethod"),
            error = "Invalid usage of @Setup: Method `internalMethod` is internal."
        )
    }


    @Test
    fun testSetupWithProtected() {
        executeBenchmark(
            annotations = listOf("setup" to "protectedMethod"),
            error = "Invalid usage of @Setup: Method `protectedMethod` is protected.",
            jvmSpecificError = "@Setup method should be public."
        )
    }


    @Test
    fun testTeardownWithPrivate() {
        executeBenchmark(
            annotations = listOf("teardown" to "privateMethod"),
            error = "Invalid usage of @TearDown: Method `privateMethod` is private. @TearDown methods should be public.",
            jvmSpecificError = "@TearDown method should be public."
        )
    }


    @Test
    fun testTeardownWithProtected() {
        executeBenchmark(
            annotations = listOf("teardown" to "protectedMethod"),
            error = "Invalid usage of @TearDown: Method `protectedMethod` is protected.",
            jvmSpecificError = "@TearDown method should be public."
        )
    }

    @Test
    fun testTeardownWithFinal() {
        executeBenchmark(
            annotations = listOf("teardown" to "finalMethod")
        )
    }

    @Test
    fun testTeardownWithInternal() {
        executeBenchmark(
            annotations = listOf("teardown" to "internalMethod"),
            error = "Invalid usage of @TearDown: Method `internalMethod` is internal."
        )
    }


    @Test
    fun testBenchmarkAndSetup() {
        executeBenchmark(
            annotations = listOf(
                "benchmark" to "plainMethod",
                "setup" to "plainMethod"
            )
        )
    }


    @Test
    fun testBenchmarkAndTearDown() {
        executeBenchmark(
            annotations = listOf(
                "benchmark" to "plainMethod",
                "teardown" to "plainMethod"
            )
        )
    }

    @Test
    fun testWithSetupAndTearDown() {
        executeBenchmark(
            annotations = listOf(
                "setup" to "plainMethod",
                "teardown" to "plainMethod"
            )
        )
    }

    @Test
    fun testBenchmarkWithBlackhole() {
        executeBenchmark(
            annotations = listOf("benchmark" to "blackholeFunction")
        )
    }

    @Test
    fun testSetupWithArg() {
        executeBenchmark(
            annotations = listOf("setup" to "methodWithArg"),
            error = "Invalid usage of @Setup: Method `methodWithArg` has a parameter of type `Int` which is neither a Blackhole nor a @State object",
            jvmSpecificError = "Method parameters should be either @State classes"
        )
    }

    @Test
    fun testTeardownWithArg() {
        executeBenchmark(
            annotations = listOf("teardown" to "methodWithArg"),
            error = "Invalid usage of @Teardown: Method `methodWithArg` has a parameter of type `Int` which is neither a Blackhole nor a @State object",
            jvmSpecificError = "Method parameters should be either @State classes"
        )
    }

    @Test
    fun testBenchmarkWithArg() {
        executeBenchmark(
            annotations = listOf("benchmark" to "methodWithArg"),
            error = "Invalid usage of @Benchmark: Method `methodWithArg` has a parameter of type `Int` which is neither a Blackhole nor a @State object",
            jvmSpecificError = "Method parameters should be either @State classes"
        )
    }
}
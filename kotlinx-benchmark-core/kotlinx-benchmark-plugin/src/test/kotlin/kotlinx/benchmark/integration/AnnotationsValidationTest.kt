package kotlinx.benchmark.integration

import kotlin.test.*

class AnnotationsValidationTest : GradleTest() {

    private val platformBenchmarkTask = "nativeBenchmark"

    private fun executeBenchmark(
        benchmarkFunction: String? = null,
        setupFunction: String? = null,
        teardownFunction: String? = null,
        paramProperty: Pair<String, List<String>>? = null,
        error: String? = null,
        jvmSpecificError: String? = null
    ) {
        val runner = project("annotations-validation")
        runner.addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
            benchmarkFunction?.let { benchmark(it) }
            setupFunction?.let { setup(it) }
            teardownFunction?.let { teardown(it) }
            paramProperty?.let { param(it.first, *it.second.toTypedArray()) }
        }

        runPlatformBenchmark(runner, error)
        runJvmBenchmark(runner, jvmSpecificError)
    }

    private fun runPlatformBenchmark(runner: Runner, error: String?) {
        if (error != null) {
            runner.runAndFail(platformBenchmarkTask) {
                assertOutputContains(error)
            }
        } else {
            runner.run(platformBenchmarkTask) {
                assertOutputContains("BUILD SUCCESSFUL")
            }
        }
    }

    private fun runJvmBenchmark(runner: Runner, jvmSpecificError: String?) {
        if (jvmSpecificError != null) {
            runner.runAndFail("jvmBenchmark") {
                assertOutputContains(jvmSpecificError)
            }
        } else {
            runner.run("jvmBenchmark") {
                assertOutputContains("BUILD SUCCESSFUL")
            }
        }
    }

    // @Param

    @Test
    fun testParamEmptyArguments() {
        executeBenchmark(
            benchmarkFunction = "plainFunction",
            paramProperty = "varProperty" to listOf(),
            error = "@Param annotation should have at least one argument. " +
                    "The annotation on property `varProperty` has no arguments.",
            jvmSpecificError = "@Param should provide the default parameters."
        )
    }

    @Test
    fun testParamValProperty() {
        executeBenchmark(
            benchmarkFunction = "plainFunction",
            paramProperty = "valProperty" to listOf("a"),
            error = "@Param property should be mutable (var). Property `valProperty` is read-only (val).",
            jvmSpecificError = "@Param annotation is not acceptable on final fields."
        )
    }

    @Test
    fun testParamPrivateProperty() {
        executeBenchmark(
            benchmarkFunction = "plainFunction",
            paramProperty = "privateProperty" to listOf("a"),
            error = "@Param property should be public. Property `privateProperty` is private."
        )
    }

    @Test
    fun testParamInternalProperty() {
        executeBenchmark(
            benchmarkFunction = "plainFunction",
            paramProperty = "internalProperty" to listOf("a"),
            error = "@Param property should be public. Property `internalProperty` is internal."
        )
    }

    @Test
    fun testParamProtectedProperty() {
        executeBenchmark(
            benchmarkFunction = "plainFunction",
            paramProperty = "protectedProperty" to listOf("a"),
            error = "@Param property should be public. Property `protectedProperty` is protected."
        )
    }

    @Test
    fun testParamFinalProperty() {
        executeBenchmark(
            benchmarkFunction = "plainFunction",
            paramProperty = "finalProperty" to listOf("a")
        )
    }

    @Test
    fun testParamNullableStringProperty() {
        executeBenchmark(
            benchmarkFunction = "plainFunction",
            paramProperty = "nullableStringProperty" to listOf("a")
        )
    }

    @Test
    fun testParamNullableIntProperty() {
        executeBenchmark(
            benchmarkFunction = "plainFunction",
            paramProperty = "nullableIntProperty" to listOf("1")
        )
    }

    @Test
    fun testParamNullableUIntProperty() {
        executeBenchmark(
            benchmarkFunction = "plainFunction",
            paramProperty = "nullableUIntProperty" to listOf("1"),
            jvmSpecificError = "@Param can only be placed over the annotation-compatible types: primitives, primitive wrappers, Strings, or enums."
        )
    }

    @Test
    fun testParamNotSupportedTypeProperty() {
        executeBenchmark(
            benchmarkFunction = "plainFunction",
            paramProperty = "notSupportedTypeProperty" to listOf("a"),
            error = "@Param property should have a primitive or string type. Property `notSupportedTypeProperty` type is `Regex`.",
            jvmSpecificError = "@Param can only be placed over the annotation-compatible types: primitives, primitive wrappers, Strings, or enums."
        )
    }

    // @Benchmark

    @Test
    fun testBenchmarkPrivateFunction() {
        executeBenchmark(
            benchmarkFunction = "privateFunction",
            error = "@Benchmark function should be public. Function `privateFunction` is private.",
            jvmSpecificError = "@Benchmark method should be public."
        )
    }

    @Test
    fun testBenchmarkInternalFunction() {
        executeBenchmark(
            benchmarkFunction = "internalFunction",
            error = "@Benchmark function should be public. Function `internalFunction` is internal."
        )
    }

    @Test
    fun testBenchmarkProtectedFunction() {
        executeBenchmark(
            benchmarkFunction = "protectedFunction",
            error = "@Benchmark function should be public. Function `protectedFunction` is protected.",
            jvmSpecificError = "@Benchmark method should be public."
        )
    }

    @Test
    fun testBenchmarkFinalFunction() {
        executeBenchmark(
            benchmarkFunction = "finalFunction"
        )
    }

    @Test
    fun testBenchmarkFunctionWithBlackholeArgument() {
        executeBenchmark(
            benchmarkFunction = "functionWithBlackholeArgument"
        )
    }

    @Test
    fun testBenchmarkFunctionWithTwoBlackholeArguments() {
        executeBenchmark(
            benchmarkFunction = "functionWithTwoBlackholeArguments",
            error = "@Benchmark function can have at most one parameter of type `Blackhole`. " +
                    "Function `functionWithTwoBlackholeArguments` has 2 parameters."
        )
    }

    @Test
    fun testBenchmarkFunctionWithIntArgument() {
        executeBenchmark(
            benchmarkFunction = "functionWithIntArgument",
            error = "@Benchmark function can have at most one parameter of type `Blackhole`. " +
                    "Function `functionWithIntArgument` has a parameter of type `Int`.",

            jvmSpecificError = "Method parameters should be either @State classes"
        )
    }

    // @Setup

    @Test
    fun testSetupPrivateFunction() {
        executeBenchmark(
            benchmarkFunction = "plainFunction",
            setupFunction = "privateFunction",
            error = "@Setup function should be public. Function `privateFunction` is private.",
            jvmSpecificError = "@Setup method should be public."
        )
    }

    @Test
    fun testSetupInternalFunction() {
        executeBenchmark(
            benchmarkFunction = "plainFunction",
            setupFunction = "internalFunction",
            error = "@Setup function should be public. Function `internalFunction` is internal."
        )
    }

    @Test
    fun testSetupProtectedFunction() {
        executeBenchmark(
            benchmarkFunction = "plainFunction",
            setupFunction = "protectedFunction",
            error = "@Setup function should be public. Function `protectedFunction` is protected.",
            jvmSpecificError = "@Setup method should be public."
        )
    }

    @Test
    fun testSetupFinalFunction() {
        executeBenchmark(
            benchmarkFunction = "plainFunction",
            setupFunction = "finalFunction"
        )
    }

    @Test
    fun testSetupFunctionWithBlackholeArgument() {
        executeBenchmark(
            benchmarkFunction = "plainFunction",
            setupFunction = "functionWithBlackholeArgument",
            error = "@Setup function should have no parameters. Function `functionWithBlackholeArgument` has 1 parameter."
        )
    }

    @Test
    fun testSetupFunctionWithTwoBlackholeArguments() {
        executeBenchmark(
            benchmarkFunction = "plainFunction",
            setupFunction = "functionWithTwoBlackholeArguments",
            error = "@Setup function should have no parameters. Function `functionWithTwoBlackholeArguments` has 2 parameters."
        )
    }

    @Test
    fun testSetupFunctionWithIntArgument() {
        executeBenchmark(
            benchmarkFunction = "plainFunction",
            setupFunction = "functionWithIntArgument",
            error = "@Setup function should have no parameters. Function `functionWithIntArgument` has 1 parameter.",
            jvmSpecificError = "Method parameters should be either @State classes"
        )
    }

    // TearDown

    @Test
    fun testTeardownPrivateFunction() {
        executeBenchmark(
            benchmarkFunction = "plainFunction",
            teardownFunction = "privateFunction",
            error = "@TearDown function should be public. Function `privateFunction` is private.",
            jvmSpecificError = "@TearDown method should be public."
        )
    }

    @Test
    fun testTeardownInternalFunction() {
        executeBenchmark(
            benchmarkFunction = "plainFunction",
            teardownFunction = "internalFunction",
            error = "@TearDown function should be public. Function `internalFunction` is internal."
        )
    }

    @Test
    fun testTeardownProtectedFunction() {
        executeBenchmark(
            benchmarkFunction = "plainFunction",
            teardownFunction = "protectedFunction",
            error = "@TearDown function should be public. Function `protectedFunction` is protected.",
            jvmSpecificError = "@TearDown method should be public."
        )
    }

    @Test
    fun testTeardownFinalFunction() {
        executeBenchmark(
            benchmarkFunction = "plainFunction",
            teardownFunction = "finalFunction"
        )
    }

    @Test
    fun testTeardownFunctionWithBlackholeArgument() {
        executeBenchmark(
            benchmarkFunction = "plainFunction",
            teardownFunction = "functionWithBlackholeArgument",
            error = "@TearDown function should have no parameters. Function `functionWithBlackholeArgument` has 1 parameter."
        )
    }

    @Test
    fun testTeardownFunctionWithTwoBlackholeArguments() {
        executeBenchmark(
            benchmarkFunction = "plainFunction",
            teardownFunction = "functionWithTwoBlackholeArguments",
            error = "@TearDown function should have no parameters. Function `functionWithTwoBlackholeArguments` has 2 parameters."
        )
    }

    @Test
    fun testTeardownFunctionWithIntArgument() {
        executeBenchmark(
            benchmarkFunction = "plainFunction",
            teardownFunction = "functionWithIntArgument",
            error = "@TearDown function should have no parameters. Function `functionWithIntArgument` has 1 parameter.",
            jvmSpecificError = "Method parameters should be either @State classes"
        )
    }

    // Mix

    @Test
    fun testBenchmarkAndSetup() {
        executeBenchmark(
            benchmarkFunction = "plainFunction",
            setupFunction = "plainFunction"
        )
    }

    @Test
    fun testBenchmarkAndTearDown() {
        executeBenchmark(
            benchmarkFunction = "plainFunction",
            teardownFunction = "plainFunction"
        )
    }

    @Test
    fun testSetupAndTearDown() {
        executeBenchmark(
            benchmarkFunction = "plainFunction",
            setupFunction = "plainFunction",
            teardownFunction = "plainFunction"
        )
    }
}
package kotlinx.benchmark.integration

import org.gradle.testkit.runner.BuildResult
import kotlin.test.*

class ConfigurationCacheTest : GradleTest() {
    private fun runConfigurationCacheTest(projectName: String, invokedTasks: List<String>, executedTasks: List<String>) {
        val project = project(projectName, gradleVersion = GradleTestVersion.v8_0) {
            // this test doesn't pass for K/N on Gradle 8.1 yet: https://youtrack.jetbrains.com/issue/KT-58063
            configuration("main") {
                warmups = 1
                iterations = 1
                iterationTime = 100
                iterationTimeUnit = "ms"
                advanced("jmhIgnoreLock", true)
            }
        }

        project.runAndSucceed(*invokedTasks.toTypedArray(), "--configuration-cache") {
            assertTasksExecuted(invokedTasks + executedTasks)
            assertConfigurationCacheStored()
        }
        project.runAndSucceed("clean", "--configuration-cache") {
            assertConfigurationCacheStored()
        }
        project.runAndSucceed(*invokedTasks.toTypedArray(), "--configuration-cache") {
            assertTasksExecuted(invokedTasks + executedTasks)
            assertConfigurationCacheReused()
        }
        project.runAndSucceed(*invokedTasks.toTypedArray(), "--configuration-cache") {
            assertTasksUpToDate(executedTasks)
            assertConfigurationCacheReused()
        }
    }

    @Test
    fun testConfigurationCacheNative() = runConfigurationCacheTest(
        "kotlin-multiplatform",
        listOf(":nativeBenchmark"),
        listOf(":compileKotlinNative", ":nativeBenchmarkGenerate", ":compileNativeBenchmarkKotlinNative", ":linkNativeBenchmarkDebugExecutableNative")
    )

    @Test
    fun testConfigurationCacheJs() = runConfigurationCacheTest(
        "kotlin-multiplatform",
        listOf(":jsBenchmark"),
        listOf(":compileKotlinJs", ":jsBenchmarkGenerate", ":compileJsBenchmarkProductionExecutableKotlinJs")
    )

    @Test
    fun testConfigurationCacheJvm() = runConfigurationCacheTest(
        "kotlin-multiplatform",
        listOf(":jvmBenchmark"),
        listOf(":compileKotlinJvm", ":jvmBenchmarkGenerate", ":jvmBenchmarkCompile")
    )

    @Test
    fun testConfigurationCacheWasm() = runConfigurationCacheTest(
        "kotlin-multiplatform",
        listOf(":wasmJsBenchmark"),
        listOf(":compileKotlinWasmJs", ":wasmJsBenchmarkGenerate", ":compileWasmJsBenchmarkProductionExecutableKotlinWasmJs")
    )
}

private fun BuildResult.assertConfigurationCacheStored() {
    assertOutputContains("Configuration cache entry stored.")
}

private fun BuildResult.assertConfigurationCacheReused() {
    assertOutputContains("Configuration cache entry reused.")
}

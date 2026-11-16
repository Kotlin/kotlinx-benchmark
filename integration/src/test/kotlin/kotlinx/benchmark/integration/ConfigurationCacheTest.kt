package kotlinx.benchmark.integration

import org.gradle.testkit.runner.BuildResult
import java.util.jar.JarFile
import kotlin.test.*

class ConfigurationCacheTest : GradleTest() {
    private fun runConfigurationCacheTest(
        projectName: String,
        invokedTasks: List<String>,
        executedTasks: List<String>,
        verify: () -> Unit = {}
    ) {
        val project = project(projectName) {
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
        verify()
        project.runAndSucceed("clean", "--configuration-cache") {
            assertConfigurationCacheStored()
        }
        project.runAndSucceed(*invokedTasks.toTypedArray(), "--configuration-cache") {
            assertTasksExecuted(invokedTasks + executedTasks)
            assertConfigurationCacheReused()
        }
        verify()
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
    fun testConfigurationCacheWasmJs() = runConfigurationCacheTest(
        "kotlin-multiplatform",
        listOf(":wasmJsBenchmark"),
        listOf(":compileKotlinWasmJs", ":wasmJsBenchmarkGenerate", ":compileWasmJsBenchmarkProductionExecutableKotlinWasmJs")
    )

    @Test
    fun testConfigurationCacheWasmWasi() = runConfigurationCacheTest(
        "kotlin-multiplatform",
        listOf(":wasmWasiBenchmark"),
        listOf(":compileKotlinWasmWasi", ":wasmWasiBenchmarkGenerate", ":compileWasmWasiBenchmarkProductionExecutableKotlinWasmWasi")
    )

    @Test
    fun testConfigurationCacheJvmJar() = runConfigurationCacheTest(
        "kotlin-multiplatform",
        listOf(":jvmBenchmarkJar"),
        listOf(":compileKotlinJvm", ":jvmBenchmarkGenerate", ":jvmBenchmarkCompile")
    ) {
        assertBenchmarkJarIsComplete()
    }

    private fun assertBenchmarkJarIsComplete() {
        val jarDir = file("build/benchmarks/jvm/jars")
        val jar = jarDir.listFiles().orEmpty().singleOrNull { it.extension == "jar" }
        assertNotNull(jar, "No benchmark jar in $jarDir")

        JarFile(jar).use { jarFile ->
            val entries = jarFile.entries().toList().map { it.name }
            // The module's own classes, and the dependencies the jar unpacks.
            for (entry in listOf("test/CommonBenchmark.class", "org/openjdk/jmh/Main.class", "kotlin/Unit.class")) {
                assertContains(entries, entry, "Jar $jar does not contain $entry")
            }
            assertEquals(
                "org.openjdk.jmh.Main",
                jarFile.manifest.mainAttributes.getValue("Main-Class"),
            )
        }
    }
}

private fun BuildResult.assertConfigurationCacheStored() {
    assertOutputContains("Configuration cache entry stored.")
}

private fun BuildResult.assertConfigurationCacheReused() {
    assertOutputContains("Configuration cache entry reused.")
}

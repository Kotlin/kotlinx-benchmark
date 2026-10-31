package kotlinx.benchmark.integration

import org.gradle.testkit.runner.BuildResult
import java.io.File
import java.util.jar.JarFile
import kotlin.test.*

class ConfigurationCacheTest : GradleTest() {
    private fun runConfigurationCacheTest(projectName: String, invokedTasks: List<String>, executedTasks: List<String>) {
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
        project.runAndSucceed("clean", "--configuration-cache") {
            assertConfigurationCacheStored()
        }
        project.runAndSucceed(*invokedTasks.toTypedArray(), "--configuration-cache") {
            assertTasksExecuted(invokedTasks + executedTasks)
            assertConfigurationCacheReused()
        }
        project.runAndSucceed(*invokedTasks.toTypedArray(), "--configuration-cache") {
            assertTasksUpToDate(executedTasks)
            assertTasksExecuted(invokedTasks)
            assertConfigurationCacheReused()
        }

        assertEquals(2, reportDirectories("main").size, "Every run must report into its own directory")
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
    fun testConfigurationCacheReportDirectoryIsNotReused() {
        val project = project("kotlin-multiplatform") {
            configuration("main") {
                warmups = 1
                iterations = 1
                iterationTime = 100
                iterationTimeUnit = "ms"
                advanced("jmhIgnoreLock", true)
            }
        }

        project.runAndSucceed(":jvmBenchmark", "--configuration-cache") {
            assertConfigurationCacheStored()
        }
        repeat(2) {
            project.runAndSucceed(":jvmBenchmark", "--configuration-cache") {
                assertConfigurationCacheReused()
            }
        }

        assertEquals(3, reportDirectories("main").size, "Every run must report into its own directory")
    }

    @Test
    fun testConfigurationCacheRecreatesBenchmarkConfigFile() {
        val project = project("kotlin-multiplatform") {
            configuration("main") {
                warmups = 1
                iterations = 1
                iterationTime = 100
                iterationTimeUnit = "ms"
                advanced("jmhIgnoreLock", true)
            }
        }

        project.runAndSucceed(":jvmBenchmark", "--configuration-cache") {
            assertConfigurationCacheStored()
        }
        val configFile = file("build/tmp/jvmBenchmark/benchmarks.txt")
        assertTrue(configFile.exists(), "Benchmark parameters must be written into the task temporary directory")

        assertTrue(file("build/tmp").deleteRecursively(), "Failed to delete build/tmp")
        project.runAndSucceed(":jvmBenchmark", "--configuration-cache") {
            assertConfigurationCacheReused()
        }
        assertTrue(configFile.exists(), "Benchmark parameters must be rewritten on a reused entry")
    }

    private fun reportDirectories(configuration: String): List<File> =
        file("build/reports/benchmarks/$configuration").listFiles().orEmpty().filter { it.isDirectory }

    @Test
    fun testJvmBenchmarkJarConfigurationCache() {
        val project = project("kotlin-multiplatform")

        project.runAndSucceed(":jvmBenchmarkJar", "--configuration-cache") {
            assertConfigurationCacheStored()
        }
        assertBenchmarkJarIsComplete()

        project.runAndSucceed("clean", "--configuration-cache")
        project.runAndSucceed(":jvmBenchmarkJar", "--configuration-cache") {
            assertConfigurationCacheReused()
            assertTasksExecuted(":jvmBenchmarkJar")
        }
        assertBenchmarkJarIsComplete()
    }

    private fun assertBenchmarkJarIsComplete() {
        val jarDir = file("build/benchmarks/jvm/jars")
        val jar = jarDir.listFiles().orEmpty().singleOrNull { it.extension == "jar" }
        assertNotNull(jar, "No benchmark jar in $jarDir")

        val entries = JarFile(jar).use { jarFile -> jarFile.entries().toList().map { it.name } }
        // The module's own classes, and the dependencies the jar unpacks.
        for (entry in listOf("test/CommonBenchmark.class", "org/openjdk/jmh/Main.class", "kotlin/Unit.class")) {
            assertTrue(entry in entries, "Jar $jar does not contain $entry")
        }
    }
}

private fun BuildResult.assertConfigurationCacheStored() {
    assertOutputContains("Configuration cache entry stored.")
}

private fun BuildResult.assertConfigurationCacheReused() {
    assertOutputContains("Configuration cache entry reused.")
}

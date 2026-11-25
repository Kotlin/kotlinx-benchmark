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
    fun testNativeReportOutputOnCacheReuse() {
        val project = project("kotlin-multiplatform") {
            configuration("main") {
                warmups = 0
                iterations = 1
                iterationTime = 100
                iterationTimeUnit = "ms"
            }
        }
        file("build.gradle").appendText(
            "\n" + """
            tasks.register('collectNativeReport', Copy) {
                from tasks.named('nativeBenchmark')
                into layout.buildDirectory.dir('collected')
            }
            """.trimIndent()
        )

        val collected = file("build/collected/native.json")
        project.runAndSucceed("collectNativeReport", "--configuration-cache") {
            assertConfigurationCacheStored()
        }
        val first = reports("main").single()
        val firstContents = first.readText()
        assertEquals(firstContents, collected.readText())

        project.runAndSucceed("collectNativeReport", "--configuration-cache") {
            assertConfigurationCacheReused()
            assertTasksExecuted(listOf(":nativeBenchmark"))
        }
        val second = reports("main").single { it != first }
        assertEquals(second.readText(), collected.readText())
        assertEquals(firstContents, first.readText())
    }

    @Test
    fun testReportTimestampPerProject() {
        val project = project("kotlin-multiplatform") {
            configuration("main") {
                warmups = 0
                iterations = 1
                iterationTime = 100
                iterationTimeUnit = "ms"
            }
        }
        file("settings.gradle").appendText("\ninclude 'other'\n")
        file("src/commonMain").copyRecursively(file("other/src/commonMain"))
        file("build.gradle").copyTo(file("other/build.gradle"))

        project.runAndSucceed(":jvmBenchmark", ":nativeBenchmark", ":other:nativeBenchmark", "--max-workers=1", "--configuration-cache") {
            assertConfigurationCacheStored()
        }
        val rootReports = reports("main")
        assertEquals(setOf("jvm.json", "native.json"), rootReports.map { it.name }.toSet())
        val rootTimestamp = rootReports.map { it.parentFile.name }.distinct().single()
        val otherReport = file("other/build/reports/benchmarks/main").walkTopDown().single { it.name == "native.json" }
        assertNotEquals(rootTimestamp, otherReport.parentFile.name)
    }

    @Test
    fun testConfigurationCacheJs() = runConfigurationCacheTest(
        "kotlin-multiplatform",
        listOf(":jsBenchmark"),
        listOf(":compileKotlinJs", ":jsBenchmarkGenerate", ":compileJsBenchmarkProductionExecutableKotlinJs")
    )

    @Test
    fun testConfigurationCacheJvm() {
        val reportPaths = mutableSetOf<String>()
        runConfigurationCacheTest(
            "kotlin-multiplatform",
            listOf(":jvmBenchmark"),
            listOf(":compileKotlinJvm", ":jvmBenchmarkGenerate", ":jvmBenchmarkCompile")
        ) {
            val report = reports("main").single { it.name == "jvm.json" }
            assertTrue(reportPaths.add(report.absolutePath), "Reused report path: $report")
        }
    }

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

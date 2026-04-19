package kotlinx.benchmark.integration

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test that the Android Benchmark project generation works as expected.
 * Tests here should _NOT_ rely on an Android emulator being present.
 *
 * TODO Figure out how to setup CI to run tests requiring an emulator.
 *  These tests should probably be in their own class, so they are easier to
 *  filter.
 *
 * TODO Add tests for different version of AGP as their API's are changing
 *  quite a bit. Defer this until Target JDK issue has been resolved.
 */
class AndroidProjectGeneratorTest: GradleTest() {

    @Test
    fun generateAndroidProjectFromResourcesGradle8_0() {
        project(
            name = "android",
            print = true,
            androidSupport = true,
            gradleVersion = GradleTestVersion.v8_0
        ).apply {
            runAndFail("androidBenchmarkGenerate")
        }
    }

    @Test
    fun generateAndroidProjectFromResourcesGradle8_7() {
        project(
            name = "android",
            print = true,
            androidSupport = true,
            gradleVersion = GradleTestVersion.v8_7
        ).apply {
            runAndSucceed("androidBenchmarkGenerate")
            assertProjectGenerated()
        }
    }

    @Test
    fun generateAndroidProjectFromResourcesGradle9() {
        project(
            name = "android",
            print = true,
            androidSupport = true,
            gradleVersion = GradleTestVersion.v9_3_0
        ).apply {
            runAndSucceed("androidBenchmarkGenerate")
            assertProjectGenerated()
        }
    }

    private fun Runner.assertProjectGenerated() {
        generatedAndroidDir("android", "") { generatedAndroidDir ->
            assertTrue(generatedAndroidDir.exists(), "Generated Android project does not exist")
            val benchmarkGradleFile = generatedAndroidDir.resolve("microbenchmark/build.gradle.kts")
            assertTrue(benchmarkGradleFile.exists(), "Generated Microbenchmark build.gradle.kts file does not exists")
            assertFalse(benchmarkGradleFile.readText().contains("<<BENCHMARKED_AAR_ABSOLUTE_PATH>>"), "Template markers have not been replaced")
        }
    }
}
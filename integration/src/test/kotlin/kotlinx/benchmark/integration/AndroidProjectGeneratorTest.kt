package kotlinx.benchmark.integration

import kotlin.test.*

/**
 * Test that the Android Benchmark project generation works as expected.
 * Tests here should _NOT_ rely on an Android emulator being present.
 *
 * The compatibility matrix between JDK, Gradle and Android Gradle Plugin
 * is rather complex. Instead of testing all permutations, this class focus
 * on key versions where we know that things are changing.
 *
 * AGP requirements for Gradle and JDK are described in the their release notes:
 * https://developer.android.com/build/releases/past-releases
 *
 * TODO Figure out how to setup CI to run tests requiring an emulator.
 *  These tests should probably be in their own class, so they are easier to
 *  filter.
 */
class AndroidProjectGeneratorTest: GradleTest() {

    // `id("com.android.kotlin.multiplatform.library")` was added in AGP 8.2, any version before that should fail
    // to compile.
    @Test
    fun generateBenchmarkProjectAGP8_0() {
        project(
            name = "android-agp8-min",
            print = true,
            jvmToolchain = 17,
            agpVersion = "8.0.0",
            gradleVersion = GradleTestVersion.v8_0
        ).apply {
            runAndFail("androidBenchmarkGenerate")
        }
    }

    // Minimum version of AGP and Gradle that supports `id("com.android.kotlin.multiplatform.library")`
    @Test
    fun generateBenchmarkProjectAGP8_2() {
        project(
            name = "android-agp8-min",
            print = true,
            jvmToolchain = 17,
            agpVersion = "8.2.0",
            gradleVersion = GradleTestVersion.v8_2
        ).apply {
            runAndSucceed("androidBenchmarkGenerate")
            assertProjectGenerated()
        }
    }

    // In AGP 8.8, the new `androidDeviceTest` source set was added, replacing
    // `androidTest`, but both still being allowed.
    @Test
    fun generateBenchmarkProjectAGP_8_8() {
        project(
            name = "android-agp8",
            print = true,
            jvmToolchain = 17,
            agpVersion = "8.8.0",
            gradleVersion = GradleTestVersion.v8_10_2
        ).apply {
            runAndSucceed("androidBenchmarkGenerate")
            assertProjectGenerated()
        }
    }

    // AGP 9 broke some API's that previously worked on AGP 8 and changed
    // how the Android target was defined.
    @Test
    fun generateBenchmarkProjectAGP_9_1() {
        project(
            name = "android-agp9",
            print = true,
            jvmToolchain = 17,
            agpVersion = "9.1.1",
            gradleVersion = GradleTestVersion.v9_3_1,
        ).apply {
            runAndSucceed("androidBenchmarkGenerate")
            assertProjectGenerated()
        }
    }

    @Ignore // TODO Requires emulator to run
    @Test
    fun runBenchmarksAGP8_2() {
        project(
            name = "android-agp8-min",
            print = true,
            jvmToolchain = 17,
            agpVersion = "8.2.0",
            gradleVersion = GradleTestVersion.v8_2
        ).apply {
            runAndSucceed("androidBenchmark")
            assertBenchmarksCompleted()
        }
    }

    @Ignore // TODO Requires emulator to run
    @Test
    fun runBenchmarksAGP_8_8() {
        project(
            name = "android-agp8",
            print = true,
            jvmToolchain = 17,
            agpVersion = "8.8.0",
            gradleVersion = GradleTestVersion.v8_10_2
        ).apply {
            runAndSucceed("androidBenchmark")
            assertBenchmarksCompleted()
        }
    }

    @Ignore // TODO Requires emulator to run
    @Test
    fun runBenchmarkProjectAGP_9_1() {
        project(
            name = "android-agp9",
            print = true,
            jvmToolchain = 17,
            agpVersion = "9.1.1",
            gradleVersion = GradleTestVersion.v9_3_1,
        ).apply {
            runAndSucceed("androidBenchmark")
            assertBenchmarksCompleted()
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

    private fun Runner.assertBenchmarksCompleted() {
        generatedOutputDir("android", "") { generatedAndroidDir ->
            // Should contain a directory for each attached emulator, but we only care about the first one.
            val dir = generatedAndroidDir.walkTopDown().drop(1).firstOrNull { it.isDirectory } ?: fail("No reports directory found in generated Android project")
            val deviceId = dir.name
            assertTrue(generatedAndroidDir.resolve("$deviceId.txt").exists(), "Missing combined output file: $deviceId.txt")
            assertTrue(generatedAndroidDir.resolve("$deviceId-test.CommonBenchmark.json").exists(), "Missing json data file: $deviceId-test.CommonBenchmark.json")
        }
    }
}
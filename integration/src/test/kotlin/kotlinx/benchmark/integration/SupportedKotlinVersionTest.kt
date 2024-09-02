package kotlinx.benchmark.integration

import kotlin.test.Test
import kotlin.test.assertEquals

class SupportedKotlinVersionTest : GradleTest() {

    /** The minimum Kotlin version that kotlinx-benchmark plugin supports. */
    private val minSupportedKotlinVersion = System.getProperty("minSupportedKotlinVersion")
    private val warningMessage =
        "JetBrains Gradle Benchmarks plugin requires Kotlin version ${GradleTestVersion.MinSupportedKotlinVersion.versionString}"

    @Test
    fun `test MinSupportedKotlinVersion matches the version used in build scripts`() {
        assertEquals(minSupportedKotlinVersion, GradleTestVersion.MinSupportedKotlinVersion.versionString)
    }

    @Test
    fun `when using min supported Kotlin version, expect no warning`() {
        val runner = project("kotlin-multiplatform", kotlinVersion = GradleTestVersion.MinSupportedKotlinVersion.versionString)

        runner.runAndSucceed(":help", "-q") {
            assertOutputDoesNotContain(warningMessage)
        }
    }

    @Test
    fun `when using unsupported Gradle version, expect warning`() {
        val runner = project("kotlin-multiplatform", kotlinVersion = GradleTestVersion.UnsupportedKotlinVersion.versionString)

        runner.run(":help", "-q") {
            assertOutputContains(warningMessage)
        }
    }
}

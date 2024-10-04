package kotlinx.benchmark.integration

import org.gradle.util.GradleVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SupportedGradleVersionTest : GradleTest() {

    /** The min supported version used in build scripts, provided as a system property. */
    private val minSupportedGradleVersion = System.getProperty("minSupportedGradleVersion")
    private val unsupportedGradleVersionWarningMessage =
        "JetBrains Gradle Benchmarks plugin requires Gradle version ${GradleTestVersion.MinSupportedGradleVersion.versionString}"
    private val incompatibleKotlinAndGradleVersionsErrorMessage =
        "The applied Kotlin Gradle is not compatible with the used Gradle version (Gradle ${GradleTestVersion.UnsupportedGradleVersion.versionString})"

    @Test
    fun `test MinSupportedGradleVersion matches the version used in build scripts`() {
        assertEquals(minSupportedGradleVersion, GradleTestVersion.MinSupportedGradleVersion.versionString)
    }

    @Test
    fun `test MinSupportedGradleVersion is greater than UnsupportedGradleVersion`() {
        // verify the test data is valid
        assertTrue(
            GradleVersion.version(GradleTestVersion.MinSupportedGradleVersion.versionString) >
                    GradleVersion.version(GradleTestVersion.UnsupportedGradleVersion.versionString)
        )
    }

    @Test
    fun `when using min supported Gradle version, expect no warning`() {
        val runner = project("kotlin-multiplatform", gradleVersion = GradleTestVersion.MinSupportedGradleVersion)

        runner.runAndSucceed(":help", "-q") {
            assertOutputDoesNotContain(unsupportedGradleVersionWarningMessage)
        }
    }

    @Test
    fun `when using unsupported Gradle version, expect warning`() {
        val runner = project("kotlin-multiplatform", gradleVersion = GradleTestVersion.UnsupportedGradleVersion)

        runner.run(":help", "-q") {
            assertTrue(
                // When kotlinx-benchmark-plugin has the same minimum supported Gradle version as KGP, reported by KGP
                output.contains(incompatibleKotlinAndGradleVersionsErrorMessage) ||
                // When kotlinx-benchmark-plugin has a newer minimum supported Gradle version than KGP, reported by kxb
                output.contains(unsupportedGradleVersionWarningMessage)
            )
        }
    }
}

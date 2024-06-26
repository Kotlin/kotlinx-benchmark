package kotlinx.benchmark.integration

import org.gradle.util.GradleVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SupportedGradleVersionTest : GradleTest() {

    /** The min supported version used in build scripts, provided as a system property. */
    private val minSupportedGradleVersion = System.getProperty("minSupportedGradleVersion")
    private val warningMessage =
        "JetBrains Gradle Benchmarks plugin requires Gradle version ${GradleTestVersion.MinSupportedGradleVersion.versionString}"

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

        runner.run(":help", "-q") {
            assertOutputDoesNotContain(warningMessage)
        }
    }

    @Test
    fun `when using unsupported Gradle version, expect warning`() {
        val runner = project("kotlin-multiplatform", gradleVersion = GradleTestVersion.UnsupportedGradleVersion)

        runner.run(":help", "-q") {
            assertOutputContains(warningMessage)
        }
    }
}

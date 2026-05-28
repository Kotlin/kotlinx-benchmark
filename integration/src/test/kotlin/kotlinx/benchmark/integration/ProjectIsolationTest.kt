package kotlinx.benchmark.integration

import kotlin.test.Test

class ProjectIsolationTest : GradleTest() {
    @Test
    fun testIsolation() {
        // Use either 2.3.0, or a Kotlin version used to build tests, if it is a more recent one
        // (which is the case when running tests with Kotlin built from the HEAD).
        val version = KotlinTestVersion.mostRecent(
            KotlinTestVersion.Kotlin2_3_0.versionString,
            KotlinTestVersion.overriddenVersion() ?: KotlinTestVersion.Kotlin2_3_0.versionString
        )

        val runner = project(
            name = "project-isolation",
            // The test uses the most recent versions w/ project isolation support
            // Feel free to bump them in the future
            gradleVersion = GradleTestVersion.v9_3_0,
            kotlinVersion = version
        )
        runner.runAndSucceed("benchmark")
    }
}

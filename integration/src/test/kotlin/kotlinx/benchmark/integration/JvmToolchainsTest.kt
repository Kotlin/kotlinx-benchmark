package kotlinx.benchmark.integration

import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import kotlin.test.assertEquals

class JvmToolchainsTest : GradleTest() {
    @Test
    fun testHigherVersionThanInGradle() {
        val runner = project("kmp-with-toolchain/higher-version-than-in-gradle", true, GradleTestVersion.v8_0)
        runner.runAndSucceed("benchmark") {
            assertEquals(TaskOutcome.SUCCESS, task(":jvmBenchmark")!!.outcome)
            assertOutputDoesNotContain("<failure>")
        }
    }

    @Test
    fun testMinSupportedVersion() {
        val runner = project("kmp-with-toolchain/min-supported-version", true)
        runner.runAndSucceed("benchmark") {
            assertEquals(TaskOutcome.SUCCESS, task(":jvmBenchmark")!!.outcome)
            assertOutputDoesNotContain("<failure>")
        }
    }
}

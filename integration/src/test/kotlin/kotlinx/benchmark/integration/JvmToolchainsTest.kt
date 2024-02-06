package kotlinx.benchmark.integration

import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import kotlin.test.assertEquals

class JvmToolchainsTest : GradleTest() {
    @Test
    fun testJvmToolchainSetup() {
        val runner = project("kmp-with-toolchain", true, GradleTestVersion.v8_0) {
        }
        runner.run("benchmark") {
            assertEquals(TaskOutcome.SUCCESS, task(":jvmBenchmark")!!.outcome)
            assertOutputDoesNotContain("<failure>")
        }
    }
}

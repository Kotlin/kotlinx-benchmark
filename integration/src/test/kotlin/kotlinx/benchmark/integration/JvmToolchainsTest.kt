package kotlinx.benchmark.integration

import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import kotlin.test.assertEquals

class JvmToolchainsTest : GradleTest() {
    @Test
    fun testHigherVersionThanInGradle() {
        val runner = project(
            "kmp-with-toolchain/higher-version-than-in-gradle",
            print = true,
            GradleTestVersion.v8_0,
            jvmToolchain = 21
        )
        runner.runAndSucceed("benchmark") {
            assertEquals(TaskOutcome.SUCCESS, task(":jvmBenchmark")!!.outcome)
            assertOutputDoesNotContain("<failure>")
        }
    }

    @Test
    fun testMinSupportedVersion() {
        // TODO JDK_11_DISCUSS
        for (jvmToolchain in listOf(11, 17, 21)) {
            val runner = project(
                "kmp-with-toolchain/min-supported-version",
                print = true,
                jvmToolchain = jvmToolchain
            )
            runner.runAndSucceed("benchmark") {
                assertEquals(TaskOutcome.SUCCESS, task(":jvmBenchmark")!!.outcome)
                assertOutputDoesNotContain("<failure>")
            }
        }
    }
}

package kotlinx.benchmark.integration

import org.junit.Test
import kotlin.test.assertTrue

class AndroidProjectGeneratorTest: GradleTest() {
    private fun testAndroidProjectGeneration(setupBlock: Runner.() -> Unit, checkBlock: Runner.() -> Unit) {
        project("source-generation", print = true, gradleVersion = GradleTestVersion.v8_7).apply {
            setupBlock()
            runAndSucceed("androidReleaseBenchmarkGenerate")
            checkBlock()
        }
    }

    @Test
    fun generateAndroidFromResources() {
        testAndroidProjectGeneration(
            setupBlock = {
                runAndSucceed("setupReleaseAndroidProject")
            },
            checkBlock = {
                generatedAndroidDir("android", "release", "") { generatedAndroidDir ->
                    assertTrue(generatedAndroidDir.exists(), "Generated Android project does not exist")
                }
            }
        )
    }
}
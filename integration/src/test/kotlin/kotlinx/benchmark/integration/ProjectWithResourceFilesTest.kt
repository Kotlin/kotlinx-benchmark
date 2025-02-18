package kotlinx.benchmark.integration

import org.junit.Test

// Regression tests for #185
class ProjectWithResourceFilesTest : GradleTest() {
    private fun verifyFor(target: String) {
        project("project-with-resources", true).let { runner ->
            runner.runAndSucceed("${target}Benchmark")
        }
    }

    @Test
    fun js() {
        verifyFor("js")
    }

    @Test
    fun native() {
        verifyFor("native")
    }

    @Test
    fun wasmJs() {
        verifyFor("wasmJs")
    }
}

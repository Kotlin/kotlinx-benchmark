package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.internal.android.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.provider.*
import org.gradle.api.tasks.*
import kotlin.time.Duration.Companion.milliseconds

/**
 * Helper task for forwarding Gradle tasks to the generated Android benchmark project,
 * while still showing the users.
 */
internal abstract class ExecBenchmarkProjectTask : DefaultTask() {

    init {
        outputs.upToDateWhen { false }
        timeoutMs.convention(60_000L)
        showLogs.convention(true)
    }

    @get:Input
    abstract val timeoutMs: Property<Long>

    // Root directory of the generated Microbenchmark Gradle project to execute.
    @get:InputDirectory
    abstract val benchmarkProjectDir: DirectoryProperty

    @get:Input
    abstract val args: ListProperty<String>

    // If `true`, the output from the underlying command is forwarded as `lifecycle` logs,
    // otherwise they are reported as `info.
    @get:Input
    abstract val showLogs: Property<Boolean>

    @TaskAction
    fun exec() {
        val taskArgs = args.get()
        val (completed, exitCode) = runBenchmarkProjectGradleTask(
            projectDir = benchmarkProjectDir.get().asFile,
            args = taskArgs,
            showTaskOutputAsLifecycle = showLogs.get(),
            timeoutMs = timeoutMs.get()
        )
        when {
            !completed -> throw GradleException("Task `$${taskArgs.joinToString()}` failed to complete in the allocated time: ${timeoutMs.get().milliseconds}")
            exitCode != 0 -> throw GradleException("Task `$${taskArgs.joinToString()}` failed with exit code: $exitCode")
        }
    }
}

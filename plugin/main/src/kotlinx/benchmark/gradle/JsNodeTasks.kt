package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.BenchmarksPlugin.Companion.RUN_BENCHMARKS_TASKNAME
import org.gradle.api.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.js.nodejs.*
import org.jetbrains.kotlin.gradle.targets.js.npm.*

fun Project.createJsBenchmarkExecTask(
    config: BenchmarkConfiguration,
    target: JsBenchmarkTarget,
    compilation: KotlinJsCompilation
) {
    val taskName = "${target.name}${config.capitalizedName()}${BenchmarksPlugin.BENCHMARK_EXEC_SUFFIX}"
    val task = NodeJsExec.create(compilation, taskName) {
        dependsOn(compilation.runtimeDependencyFiles)

        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Executes benchmark for '${target.name}'"

        val executableFile = compilation.compileKotlinTask.outputFile
        args("-r", "source-map-support/register")
        args(executableFile.absolutePath)
        workingDir = compilation.npmProject.dir

        onlyIf { executableFile.exists() }

        val reportFile = setupReporting(target, config)
        args(writeParameters(target.name, reportFile, traceFormat(), config))
    }

    tasks.getByName(config.prefixName(RUN_BENCHMARKS_TASKNAME)).dependsOn(task)
}



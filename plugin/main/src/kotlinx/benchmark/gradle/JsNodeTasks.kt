package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.BenchmarksPlugin.Companion.RUN_BENCHMARKS_TASKNAME
import org.gradle.api.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.js.dsl.*
import org.jetbrains.kotlin.gradle.targets.js.ir.*
import org.jetbrains.kotlin.gradle.targets.js.nodejs.*

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

        nodeArgs.addAll(listOf("-r", "source-map-support/register"))

        val executableFile = when (val kotlinTarget = compilation.target) {
            is KotlinJsIrTarget -> {
                val binary = kotlinTarget.binaries.executable(compilation)
                    .first { it.mode == KotlinJsBinaryMode.PRODUCTION } as JsIrBinary
                binary.linkSyncTask.map {
                    it.destinationDir.resolve(binary.linkTask.get().outputFileProperty.get().name)
                }
            }
            else -> compilation.compileKotlinTaskProvider.map { it.outputFileProperty.get() }
        }
        inputFileProperty.set(project.layout.file(executableFile))

        val reportFile = setupReporting(target, config)
        args(writeParameters(target.name, reportFile, traceFormat(), config))
    }

    tasks.getByName(config.prefixName(RUN_BENCHMARKS_TASKNAME)).dependsOn(task)
}



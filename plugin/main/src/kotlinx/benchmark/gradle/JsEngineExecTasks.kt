package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.BenchmarksPlugin.Companion.BENCHMARKS_TASK_GROUP
import kotlinx.benchmark.gradle.BenchmarksPlugin.Companion.RUN_BENCHMARKS_TASKNAME
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.ir.JsIrBinary
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec

internal fun Project.makeBenchmarkConfigExecTask(
    config: BenchmarkConfiguration,
    target: BenchmarkTarget,
    executeTask: Provider<NodeJsExec>
) {
    val configTaskName = "${target.name}${config.capitalizedName()}${BenchmarksPlugin.BENCHMARK_EXEC_SUFFIX}"
    val configExecTask = task<DefaultTask>(configTaskName) {
        group = BENCHMARKS_TASK_GROUP
        description = "Executes benchmark for '${target.name}'"
        dependsOn(executeTask)
    }
    tasks.getByName(config.prefixName(RUN_BENCHMARKS_TASKNAME)).dependsOn(configExecTask)
}

@KotlinxBenchmarkPluginInternalApi
fun createJsEngineBenchmarkExecTask(
    config: BenchmarkConfiguration,
    target: BenchmarkTarget,
    binary: JsIrBinary,
    executableFile: Provider<RegularFile>,
): TaskProvider<NodeJsExec> {
    val compilationTarget = binary.target
    if (compilationTarget.platformType != KotlinPlatformType.js && compilationTarget.platformType != KotlinPlatformType.wasm) {
        throw GradleException("Unsupported platforms type ${compilationTarget.platformType}")
    }
    if (!compilationTarget.isNodejsConfigured) {
        throw GradleException("kotlinx-benchmark only supports nodejs() environment for KotlinJs or Kotlin/Wasm.")
    }

    val taskName = "${binary.name}${config.capitalizedName()}${BenchmarksPlugin.BENCHMARK_EXEC_SUFFIX}"
    val execTask = createNodeJsExec(config, target, binary.compilation, executableFile, taskName)
    execTask.configure { it.dependsOn(binary.linkTask) }
    return execTask
}

private val KotlinJsIrCompilation.isWasmCompilation: Boolean get() =
    target.platformType == KotlinPlatformType.wasm

private fun MutableList<String>.addJsArguments() {
    add("-r")
    add("source-map-support/register")
}

private fun createNodeJsExec(
    config: BenchmarkConfiguration,
    target: BenchmarkTarget,
    compilation: KotlinJsIrCompilation,
    executableFile: Provider<RegularFile>,
    taskName: String
): TaskProvider<NodeJsExec> = NodeJsExec.register(compilation, taskName) {
    dependsOn(compilation.runtimeDependencyFiles)
    inputFileProperty.set(executableFile)
    with(nodeArgs) {
        if (!compilation.isWasmCompilation) {
            addJsArguments()
        }
    }
    val reportFile = setupReporting(target, config)
    args(writeParameters(target.name, reportFile, traceFormat(), config))
}
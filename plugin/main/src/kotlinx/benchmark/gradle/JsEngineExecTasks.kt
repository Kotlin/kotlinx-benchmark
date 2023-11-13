package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.BenchmarksPlugin.Companion.RUN_BENCHMARKS_TASKNAME
import org.gradle.api.*
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.d8.D8Exec
import org.jetbrains.kotlin.gradle.targets.js.dsl.*
import org.jetbrains.kotlin.gradle.targets.js.ir.*
import org.jetbrains.kotlin.gradle.targets.js.nodejs.*

fun Project.createJsEngineBenchmarkExecTask(
    config: BenchmarkConfiguration,
    target: BenchmarkTarget,
    compilation: KotlinJsIrCompilation
) {
    val taskName = "${target.name}${config.capitalizedName()}${BenchmarksPlugin.BENCHMARK_EXEC_SUFFIX}"
    val compilationTarget = compilation.target

    if (compilationTarget is KotlinJsSubTargetContainerDsl) {
        compilationTarget.whenNodejsConfigured {
            val execTask = createNodeJsExec(config, target, compilation, taskName)
            tasks.getByName(config.prefixName(RUN_BENCHMARKS_TASKNAME)).dependsOn(execTask)
        }
    }

    if (compilationTarget is KotlinWasmSubTargetContainerDsl) {
        check(compilation is KotlinJsIrCompilation) { "Legacy Kotlin/JS is does not supported by D8 engine" }
        compilationTarget.whenD8Configured {
            val execTask = createD8Exec(config, target, compilation, taskName)
            tasks.getByName(config.prefixName(RUN_BENCHMARKS_TASKNAME)).dependsOn(execTask)
        }
    }
}

private fun Project.getExecutableFile(compilation: KotlinJsIrCompilation): Provider<RegularFile> {
    val kotlinTarget = compilation.target as KotlinJsIrTarget
    val binary = kotlinTarget.binaries.executable(compilation)
        .first { it.mode == KotlinJsBinaryMode.PRODUCTION } as JsIrBinary
    val outputFileName = binary.linkTask.flatMap { task ->
        task.compilerOptions.moduleName.map { "$it.js" }
    }
    val destinationDir = binary.linkSyncTask.map { it.destinationDir }
    val executableFile = destinationDir.zip(outputFileName) { dir, fileName -> dir.resolve(fileName) }
    return project.layout.file(executableFile)
}

private val KotlinJsIrCompilation.isWasmCompilation: Boolean get() =
    target.platformType == KotlinPlatformType.wasm

private fun MutableList<String>.addWasmArguments() {
    add("--experimental-wasm-gc")
    add("--experimental-wasm-eh")
}

private fun MutableList<String>.addJsArguments() {
    add("-r")
    add("source-map-support/register")
}

private fun Project.createNodeJsExec(
    config: BenchmarkConfiguration,
    target: BenchmarkTarget,
    compilation: KotlinJsIrCompilation,
    taskName: String
): TaskProvider<NodeJsExec> = NodeJsExec.create(compilation, taskName) {
    dependsOn(compilation.runtimeDependencyFiles)
    group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
    description = "Executes benchmark for '${target.name}' with NodeJS"
    inputFileProperty.set(getExecutableFile(compilation))
    with(nodeArgs) {
        if (compilation.isWasmCompilation) {
            addWasmArguments()
        } else {
            addJsArguments()
        }
    }
    val reportFile = setupReporting(target, config)
    args(writeParameters(target.name, reportFile, traceFormat(), config))
}

private fun Project.createD8Exec(
    config: BenchmarkConfiguration,
    target: BenchmarkTarget,
    compilation: KotlinJsIrCompilation,
    taskName: String
): TaskProvider<D8Exec> = D8Exec.create(compilation, taskName) {
    dependsOn(compilation.runtimeDependencyFiles)
    group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
    description = "Executes benchmark for '${target.name}' with D8"
    inputFileProperty.set(getExecutableFile(compilation))
    if (compilation.isWasmCompilation) {
        d8Args.addWasmArguments()
    }
    val reportFile = setupReporting(target, config)
    args(writeParameters(target.name, reportFile, traceFormat(), config))
    standardOutput = ConsoleAndFilesOutputStream()
}

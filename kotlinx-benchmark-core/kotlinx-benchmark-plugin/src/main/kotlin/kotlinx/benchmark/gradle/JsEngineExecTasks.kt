package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.BenchmarksPlugin.Companion.RUN_BENCHMARKS_TASKNAME
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import org.gradle.api.*
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.util.internal.VersionNumber
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.d8.D8Exec
import org.jetbrains.kotlin.gradle.targets.js.d8.D8RootExtension
import org.jetbrains.kotlin.gradle.targets.js.dsl.*
import org.jetbrains.kotlin.gradle.targets.js.ir.*
import org.jetbrains.kotlin.gradle.targets.js.nodejs.*

@KotlinxBenchmarkPluginInternalApi
fun Project.createJsEngineBenchmarkExecTask(
    config: BenchmarkConfiguration,
    target: BenchmarkTarget,
    compilation: KotlinJsIrCompilation
) {
    val taskName = "${target.name}${config.capitalizedName()}${BenchmarksPlugin.BENCHMARK_EXEC_SUFFIX}"

    val compilationTarget = compilation.target
    check(compilationTarget is KotlinJsIrTarget)

    when (compilationTarget.platformType) {
        KotlinPlatformType.wasm -> {
            if (compilationTarget.isD8Configured) {
                val execTask = createD8Exec(config, target, compilation, taskName)
                tasks.getByName(config.prefixName(RUN_BENCHMARKS_TASKNAME)).dependsOn(execTask)
            } else if (compilationTarget.isNodejsConfigured) {
                val execTask = createNodeJsExec(config, target, compilation, taskName)
                tasks.getByName(config.prefixName(RUN_BENCHMARKS_TASKNAME)).dependsOn(execTask)
            } else {
                throw GradleException("kotlinx-benchmark only supports d8() and nodejs() environments for Kotlin/Wasm.")
            }
        }
        KotlinPlatformType.js -> {
            if (compilationTarget.isNodejsConfigured) {
                val execTask = createNodeJsExec(config, target, compilation, taskName)
                tasks.getByName(config.prefixName(RUN_BENCHMARKS_TASKNAME)).dependsOn(execTask)
            } else {
                throw GradleException("kotlinx-benchmark only supports nodejs() environment for Kotlin/JS.")
            }
        }
        else -> {
            throw GradleException("Unsupported platforms type ${compilationTarget.platformType}")
        }
    }
}

private fun Project.getExecutableFile(compilation: KotlinJsIrCompilation): Provider<RegularFile> {
    val kotlinTarget = compilation.target as KotlinJsIrTarget
    val binary = kotlinTarget.binaries.executable(compilation)
        .first { it.mode == KotlinJsBinaryMode.PRODUCTION } as JsIrBinary
    val extension = if (kotlinTarget.platformType == KotlinPlatformType.wasm) "mjs" else "js"
    val outputFileName = binary.linkTask.flatMap { task ->
        task.compilerOptions.moduleName.map { "$it.$extension" }
    }
    val destinationDir = binary.linkSyncTask.flatMap { it.destinationDirectory }
    val executableFile = destinationDir.zip(outputFileName) { dir, fileName -> dir.resolve(fileName) }
    return project.layout.file(executableFile)
}

private val KotlinJsIrCompilation.isWasmCompilation: Boolean get() =
    target.platformType == KotlinPlatformType.wasm

private fun MutableList<String>.addWasmGcArguments() {
    add("--experimental-wasm-gc")
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
            val addGcArgs = rootProject.extensions.findByType(NodeJsRootExtension::class.java)?.let {
                val nodeVersion = VersionNumber.parse(it.nodeVersion)
                // Starting from version 22, NodeJS is shipped with V8 versions
                // that no longer accept --experimental-wasm-gc argument.
                nodeVersion.major < 22
            } ?: true
            if (addGcArgs) {
                addWasmGcArguments()
            }
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
        val addGcArgs = rootProject.extensions.findByType(D8RootExtension::class.java)?.let {
            val d8Version = VersionNumber.parse(it.version)
            // --experimental-wasm-gc flag was removed from V8 starting from ~ 12.3.68
            d8Version < VersionNumber(12, 3, 68, null)
        } ?: true
        if (addGcArgs) {
            d8Args.addWasmGcArguments()
        }
    }
    val reportFile = setupReporting(target, config)
    args(writeParameters(target.name, reportFile, traceFormat(), config))
    standardOutput = ConsoleAndFilesOutputStream()
}

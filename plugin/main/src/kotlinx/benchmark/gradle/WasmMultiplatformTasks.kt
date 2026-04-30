package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.internal.platform.wasm.WasmTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.ExecutableWasm
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import java.io.File

private fun patchWasiMjsFile(wasiMjsFile: RegularFile) {
    val roots = File.listRoots()
        .filter { it.canRead() }
        .map { it.absolutePath.replace("\\","\\\\") }
    val rootsPreopens = roots.joinToString(separator = ", ") { "'$it' : '$it'" }
    val wasiInitializer = "new WASI({"
    val fileContent = wasiMjsFile.asFile.readText()
    val patchedContent = fileContent.replace(wasiInitializer, "$wasiInitializer preopens: { $rootsPreopens }, ")
    wasiMjsFile.asFile.writeText(patchedContent)
}

@KotlinxBenchmarkPluginInternalApi
fun Project.processWasmCompilation(target: WasmBenchmarkTarget) {
    project.logger.info("Configuring benchmarks for '${target.name}' using Kotlin/Wasm")
    val compilation = target.compilation

    createWasmBenchmarkGenerateSourceTask(target, compilation)

    val benchmarkCompilation = createWasmBenchmarkCompileTask(target)
    benchmarkCompilation.binaries.configureEach { binary ->
        binary.linkTask.configure { linkTask ->
            if (compilation.wasmTarget == WasmTarget.WASI) {
                val mainFile = binary.mainFile
                linkTask.doLast {
                    patchWasiMjsFile(mainFile.get())
                }
            }
        }

        val fileToExecute = if (compilation.wasmTarget == WasmTarget.WASI)
            @OptIn(ExperimentalWasmDsl::class)
            (binary as ExecutableWasm).mainOptimizedFile else binary.mainFileSyncPath

        target.extension.configurations.forEach {
            val execTask = createJsEngineBenchmarkExecTask(it, target, binary, fileToExecute)
            if (binary.mode == target.buildType) {
                makeBenchmarkConfigExecTask(it, target, execTask)
            }
        }
    }
}

private fun Project.createWasmBenchmarkCompileTask(target: WasmBenchmarkTarget): KotlinJsIrCompilation {
    val compilation = target.compilation
    val benchmarkBuildDir = benchmarkBuildDir(target)
    val benchmarkCompilation =
        compilation.target.compilations.create(target.name + BenchmarksPlugin.BENCHMARK_COMPILATION_SUFFIX) as KotlinJsIrCompilation

    val kotlinTarget = compilation.target
    check(kotlinTarget is KotlinJsTargetDsl)

    kotlinTarget.binaries.executable(benchmarkCompilation)

    benchmarkCompilation.apply {
        val sourceSet = kotlinSourceSets.single()

        sourceSet.resources.setSrcDirs(files())
        sourceSet.kotlin.setSrcDirs(files("$benchmarkBuildDir/sources"))

        associateWith(compilation)

        compileTaskProvider.configure {
            it.apply {
                group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
                description = "Compile Wasm benchmark source files for '${target.name}'"
                dependsOn("${target.name}${BenchmarksPlugin.BENCHMARK_GENERATE_SUFFIX}")
            }
        }
    }
    return benchmarkCompilation
}

private fun Project.createWasmBenchmarkGenerateSourceTask(
    target: WasmBenchmarkTarget,
    compilationOutput: KotlinJsIrCompilation
) {
    val benchmarkBuildDir = benchmarkBuildDir(target)
    task<WasmSourceGeneratorTask>("${target.name}${BenchmarksPlugin.BENCHMARK_GENERATE_SUFFIX}") {
        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Generate Wasm source files for '${target.name}'"
        title = target.name
        inputClassesDirs = compilationOutput.output.classesDirs
        inputDependencies = compilationOutput.runtimeDependencyFiles
        outputResourcesDir = file("$benchmarkBuildDir/resources")
        outputSourcesDir = file("$benchmarkBuildDir/sources")
    }
}

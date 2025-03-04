package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import org.gradle.api.*
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.dsl.*
import org.jetbrains.kotlin.gradle.targets.js.ir.*

@KotlinxBenchmarkPluginInternalApi
fun Project.processWasmCompilation(target: WasmBenchmarkTarget) {
    project.logger.info("Configuring benchmarks for '${target.name}' using Kotlin/Wasm")
    val compilation = target.compilation

    createWasmBenchmarkGenerateSourceTask(target, compilation)

    val benchmarkCompilation = createWasmBenchmarkCompileTask(target)

    target.extension.configurations.forEach {
        createJsEngineBenchmarkExecTask(it, target, benchmarkCompilation, KotlinJsBinaryMode.PRODUCTION)
        createJsEngineBenchmarkExecTask(it, target, benchmarkCompilation, KotlinJsBinaryMode.DEVELOPMENT)
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
        inputClassesDirs = compilationOutput.output.allOutputs
        inputDependencies = compilationOutput.compileDependencyFiles
        outputResourcesDir = file("$benchmarkBuildDir/resources")
        outputSourcesDir = file("$benchmarkBuildDir/sources")
    }
}

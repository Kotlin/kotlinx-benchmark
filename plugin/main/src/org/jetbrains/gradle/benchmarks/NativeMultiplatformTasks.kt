package org.jetbrains.gradle.benchmarks

import org.gradle.api.*
import org.gradle.api.file.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*

fun Project.processNativeCompilation(
    extension: BenchmarksExtension,
    config: BenchmarkConfiguration,
    compilation: KotlinNativeCompilation
) {
    configureMultiplatformNativeCompilation(this, config, compilation)
    createNativeBenchmarkGenerateSourceTask(
        extension,
        config,
        compilation.target,
        compilation.compileAllTaskName,
        compilation.output.allOutputs
    )

/*
    val benchmarkCompilation = createJsBenchmarkCompileTask(extension, config, compilation)
    createJsBenchmarkDependenciesTask(extension, config, benchmarkCompilation)
    createJsBenchmarkExecTask(extension, config, benchmarkCompilation)
*/
}

private fun configureMultiplatformNativeCompilation(
    project: Project,
    config: BenchmarkConfiguration,
    compilation: KotlinNativeCompilation
) {
    // TODO: add dependency to multiplatform benchmark runtime lib
}

private fun Project.createNativeBenchmarkGenerateSourceTask(
    extension: BenchmarksExtension,
    config: BenchmarkConfiguration,
    target: KotlinNativeTarget,
    compilationTask: String,
    compilationOutput: FileCollection
) {
    val benchmarkBuildDir = benchmarkBuildDir(extension, config)
    task<NativeSourceGeneratorTask>("${config.name}${BenchmarksPlugin.BENCHMARK_GENERATE_SUFFIX}") {
        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Generate Native source files for '${config.name}'"
        // dependsOn(compilationTask) // next line should do it implicitly
        this.target = target.konanTarget.name
        inputClassesDirs = compilationOutput
        outputResourcesDir = file("$benchmarkBuildDir/resources")
        outputSourcesDir = file("$benchmarkBuildDir/sources")
    }
}

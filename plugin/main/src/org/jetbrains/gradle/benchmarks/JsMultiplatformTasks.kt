package org.jetbrains.gradle.benchmarks

import org.gradle.api.*
import org.gradle.api.file.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.tasks.*

fun Project.processJsCompilation(
    extension: BenchmarksExtension,
    config: BenchmarkConfiguration,
    compilation: KotlinJsCompilation
) {
    createJsBenchmarkInstallTask()
    configureMultiplatformJsCompilation(this, config, compilation)
    createJsBenchmarkGenerateSourceTask(
        extension,
        config,
        compilation.compileAllTaskName,
        compilation.output.allOutputs
    )

    val benchmarkCompilation = createJsBenchmarkCompileTask(extension, config, compilation)
    createJsBenchmarkDependenciesTask(extension, config, benchmarkCompilation)
    createJsBenchmarkExecTask(extension, config, benchmarkCompilation)
}

private fun Project.createJsBenchmarkCompileTask(
    extension: BenchmarksExtension,
    config: BenchmarkConfiguration,
    compilation: KotlinJsCompilation
): KotlinJsCompilation {

    val benchmarkBuildDir = benchmarkBuildDir(extension, config)
    val benchmarkCompilation = compilation.target.compilations.create(BenchmarksPlugin.BENCHMARK_COMPILATION_NAME) as KotlinJsCompilation

    benchmarkCompilation.apply {
        val sourceSet = kotlinSourceSets.single()
        sourceSet.kotlin.setSrcDirs(files("$benchmarkBuildDir/sources"))
        sourceSet.resources.setSrcDirs(files())
        sourceSet.dependencies {
            implementation(compilation.compileDependencyFiles)
            implementation(compilation.output.allOutputs)
        }
        compileKotlinTask.apply {
            group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
            description = "Compile JS benchmark source files for '${config.name}'"
            destinationDir = file("$benchmarkBuildDir/classes")
            dependsOn("${config.name}${BenchmarksPlugin.BENCHMARK_GENERATE_SUFFIX}")

            kotlinOptions.apply {
                sourceMap = true
                moduleKind = "umd"
            }
        }
    }
    return benchmarkCompilation
}

private fun Project.createJsBenchmarkGenerateSourceTask(
    extension: BenchmarksExtension,
    config: BenchmarkConfiguration,
    compilationTask: String,
    compilationOutput: FileCollection
) {
    val benchmarkBuildDir = benchmarkBuildDir(extension, config)
    task<JsSourceGeneratorTask>("${config.name}${BenchmarksPlugin.BENCHMARK_GENERATE_SUFFIX}") {
        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Generate JS source files for '${config.name}'"
        // dependsOn(compilationTask) // next line should do it implicitly
        inputClassesDirs = compilationOutput
        outputResourcesDir = file("$benchmarkBuildDir/resources")
        outputSourcesDir = file("$benchmarkBuildDir/sources")
    }
}

private fun configureMultiplatformJsCompilation(
    project: Project,
    config: BenchmarkConfiguration,
    compilation: KotlinJsCompilation
) {
    // TODO: add dependency to multiplatform benchmark runtime lib
}

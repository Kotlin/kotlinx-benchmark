package org.jetbrains.gradle.benchmarks

import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.tasks.*

fun Project.processJsCompilation(
    extension: BenchmarksExtension,
    config: BenchmarkConfiguration,
    compilation: KotlinJsCompilation
) {
    configureMultiplatformJsCompilation(this, config, compilation)
    createJsBenchmarkGenerateSourceTask(
        extension,
        config,
        compilation.compileAllTaskName,
        compilation.output.allOutputs
    )

    createJsBenchmarkCompileTask(extension, config, compilation)

}

private fun Project.createJsBenchmarkCompileTask(
    extension: BenchmarksExtension,
    config: BenchmarkConfiguration,
    compilation: KotlinJsCompilation
) {

    val benchmarkBuildDir = benchmarkBuildDir(extension, config)
    val benchmarkCompilation = compilation.target.compilations.create("benchmark")
    val compileTask = tasks.getByName(benchmarkCompilation.compileKotlinTaskName) as Kotlin2JsCompile
    compileTask.kotlinOptions.apply {
        sourceMap = true
        moduleKind = "umd"
    }

    benchmarkCompilation.apply {
        val sourceSet = kotlinSourceSets.single()
        sourceSet.kotlin.srcDir(file("$benchmarkBuildDir/sources"))
        sourceSet.dependencies {
            implementation(compilation.compileDependencyFiles)
            implementation(compilation.output.allOutputs)
        }
        compileTask.apply {
            group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
            description = "Compile JS benchmark source files for '${config.name}'"
            destinationDir = file("$benchmarkBuildDir/classes")
            dependsOn("${config.name}${BenchmarksPlugin.BENCHMARK_GENERATE_SUFFIX}")
        }
    }
}

private fun Project.createJsBenchmarkExecTask(
    extension: BenchmarksExtension,
    config: BenchmarkConfiguration,
    runtimeClasspath: FileCollection
) {
    val benchmarkBuildDir = benchmarkBuildDir(extension, config)
    task<Exec>("${config.name}${BenchmarksPlugin.BENCHMARK_EXEC_SUFFIX}", depends = "benchmark") {
        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Execute benchmark for '${config.name}'"

        val compiledOutput = file("$benchmarkBuildDir/classes")
        val resourcesOutput = file("$benchmarkBuildDir/resources")

        commandLine = "node $compiledOutput".split(' ')

        dependsOn("${config.name}${BenchmarksPlugin.BENCHMARK_COMPILE_SUFFIX}")
        tasks.getByName("benchmark").dependsOn(this)
    }
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

package org.jetbrains.gradle.benchmarks

import org.gradle.api.*
import org.gradle.api.file.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*

fun Project.processJsCompilation(config: JsBenchmarkConfiguration) {
    project.logger.info("Configuring benchmarks for '${config.name}' using Kotlin/JS")
    val compilation = config.compilation

    configureMultiplatformJsCompilation(config)

    createJsBenchmarkInstallTask()
    createJsBenchmarkGenerateSourceTask(config, compilation.output.allOutputs)

    val benchmarkCompilation = createJsBenchmarkCompileTask(config)
    createJsBenchmarkDependenciesTask(config, benchmarkCompilation)
    createJsBenchmarkExecTask(config, benchmarkCompilation)
}

private fun Project.createJsBenchmarkCompileTask(config: JsBenchmarkConfiguration): KotlinJsCompilation {
    val compilation = config.compilation
    val benchmarkBuildDir = benchmarkBuildDir(config)
    val benchmarkCompilation =
        compilation.target.compilations.create(BenchmarksPlugin.BENCHMARK_COMPILATION_NAME) as KotlinJsCompilation

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

            //TODO: fix destination dir after KT-29711 is fixed
            //println("JS: ${kotlinOptions.outputFile}")
            //destinationDir = file("$benchmarkBuildDir/classes")
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
    config: JsBenchmarkConfiguration,
    compilationOutput: FileCollection
) {
    val benchmarkBuildDir = benchmarkBuildDir(config)
    task<JsSourceGeneratorTask>("${config.name}${BenchmarksPlugin.BENCHMARK_GENERATE_SUFFIX}") {
        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Generate JS source files for '${config.name}'"
        title = config.name
        inputClassesDirs = compilationOutput
        outputResourcesDir = file("$benchmarkBuildDir/resources")
        outputSourcesDir = file("$benchmarkBuildDir/sources")
    }
}

private fun Project.configureMultiplatformJsCompilation(config: JsBenchmarkConfiguration) {
    // Add runtime library as an implementation dependency to the specified compilation
    val runtime = dependencies.create("${BenchmarksPlugin.RUNTIME_DEPENDENCY_BASE}-js:${config.extension.version}")

    config.compilation.dependencies {
        implementation(runtime)
    }
}

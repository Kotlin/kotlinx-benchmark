package org.jetbrains.gradle.benchmarks

import org.gradle.api.*
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*

fun Project.processNativeCompilation(config: NativeBenchmarkConfiguration) {
    project.logger.info("Configuring benchmarks for '${config.name}' using Kotlin/Native")
    createNativeBenchmarkGenerateSourceTask(config)

    val benchmarkCompilation = createNativeBenchmarkCompileTask(config)
    createNativeBenchmarkExecTask(config, benchmarkCompilation)
}

private fun Project.createNativeBenchmarkGenerateSourceTask(config: NativeBenchmarkConfiguration) {
    val benchmarkBuildDir = benchmarkBuildDir(config)
    task<NativeSourceGeneratorTask>("${config.name}${BenchmarksPlugin.BENCHMARK_GENERATE_SUFFIX}") {
        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Generate Native source files for '${config.name}'"

        val compilation = config.compilation
        this.target = compilation.target.konanTarget.name
        inputClassesDirs = compilation.output.allOutputs
        inputDependencies = compilation.compileDependencyFiles
        outputResourcesDir = file("$benchmarkBuildDir/resources")
        outputSourcesDir = file("$benchmarkBuildDir/sources")
    }
}

private fun Project.createNativeBenchmarkCompileTask(config: NativeBenchmarkConfiguration): KotlinNativeCompilation {

    val compilation = config.compilation
    val benchmarkBuildDir = benchmarkBuildDir(config)
    val target = compilation.target
    val benchmarkCompilation =
        target.compilations.create(BenchmarksPlugin.BENCHMARK_COMPILATION_NAME) as KotlinNativeCompilation

    // In the previous version of this method a compileTask was changed to build an executable instead of klib.
    // Currently it's impossible to change task output kind and an executable is always produced by
    // a link task. So we disable execution the klib compiling task to save time.
    val compileTask = tasks.getByName(benchmarkCompilation.compileKotlinTaskName)
    compileTask.enabled = false

    benchmarkCompilation.apply {
        val sourceSet = kotlinSourceSets.single()
        sourceSet.resources.setSrcDirs(files())
        sourceSet.kotlin.setSrcDirs(files("$benchmarkBuildDir/sources"))
        sourceSet.dependencies {
            implementation(compilation.compileDependencyFiles)
            implementation(compilation.output.allOutputs)
        }
    }

    target.apply {
        binaries {
            // The release build type is already optimized and non-debuggable.
            executable(benchmarkCompilation.name, listOf(RELEASE)) {
                this.compilation = benchmarkCompilation
                // A link task's name is linkReleaseExecutable<Target>.
                linkTask.apply {
                    group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
                    description = "Compile Native benchmark source files for '${config.name}'"
                    dependsOn("${config.name}${BenchmarksPlugin.BENCHMARK_GENERATE_SUFFIX}")

                    // It's impossible to change output directory using the binaries DSL.
                    // See https://youtrack.jetbrains.com/issue/KT-29395
                    destinationDir = file("$benchmarkBuildDir/classes")
                }
                tasks.getByName(BenchmarksPlugin.ASSEMBLE_BENCHMARKS_TASKNAME).dependsOn(linkTask)
                entryPoint("org.jetbrains.gradle.benchmarks.generated.main")
            }
        }
    }
    return benchmarkCompilation
}

fun Project.createNativeBenchmarkExecTask(
    config: NativeBenchmarkConfiguration,
    benchmarkCompilation: KotlinNativeCompilation
) {
    task<Exec>(
        "${config.name}${BenchmarksPlugin.BENCHMARK_EXEC_SUFFIX}",
        depends = BenchmarksPlugin.RUN_BENCHMARKS_TASKNAME
    ) {
        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Executes benchmark for '${config.name}'"
        val binary = benchmarkCompilation.target.binaries.getExecutable(benchmarkCompilation.name, NativeBuildType.RELEASE)
        val linkTask = binary.linkTask

        val reportsDir = benchmarkReportsDir(config)
        val reportFile = reportsDir.resolve("${config.name}.json")

        executable = linkTask.outputFile.get().absolutePath
        args = listOf(reportFile.toString(), config.iterations, config.iterationTime) // TODO: configure!
        dependsOn(linkTask)
        doFirst {
            reportsDir.mkdirs()
            logger.lifecycle("Running benchmarks for ${config.name}")
        }
    }
}

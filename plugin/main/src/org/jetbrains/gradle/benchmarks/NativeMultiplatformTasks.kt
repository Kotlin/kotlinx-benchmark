package org.jetbrains.gradle.benchmarks

import org.gradle.api.*
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import java.io.*

fun Project.processNativeCompilation(
    extension: BenchmarksExtension,
    config: BenchmarkConfiguration,
    compilation: KotlinNativeCompilation
) {
    configureMultiplatformNativeCompilation(this, config, compilation)
    createNativeBenchmarkGenerateSourceTask(extension, config, compilation)

    val benchmarkCompilation = createNativeBenchmarkCompileTask(extension, config, compilation)
    createNativeBenchmarkExecTask(extension, config, benchmarkCompilation)
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
    compilation: KotlinNativeCompilation
) {
    val benchmarkBuildDir = benchmarkBuildDir(extension, config)
    task<NativeSourceGeneratorTask>("${config.name}${BenchmarksPlugin.BENCHMARK_GENERATE_SUFFIX}") {
        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Generate Native source files for '${config.name}'"
        this.target = compilation.target.konanTarget.name
        inputClassesDirs = compilation.output.allOutputs
        inputDependencies = compilation.compileDependencyFiles
        outputResourcesDir = file("$benchmarkBuildDir/resources")
        outputSourcesDir = file("$benchmarkBuildDir/sources")
    }
}

private fun Project.createNativeBenchmarkCompileTask(
    extension: BenchmarksExtension,
    config: BenchmarkConfiguration,
    compilation: KotlinNativeCompilation
): KotlinNativeCompilation {

    val benchmarkBuildDir = benchmarkBuildDir(extension, config)
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
    extension: BenchmarksExtension,
    config: BenchmarkConfiguration,
    benchmarkCompilation: KotlinNativeCompilation
) {
    task<Exec>(
        "${config.name}${BenchmarksPlugin.BENCHMARK_EXEC_SUFFIX}",
        depends = BenchmarksPlugin.RUN_BENCHMARKS_TASKNAME
    ) {
        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Executes benchmark for '${config.name}'"
        //setScript(file("$nodeModulesDir/${compilation.output}"))
        val binary = benchmarkCompilation.target.binaries.getExecutable(benchmarkCompilation.name, NativeBuildType.RELEASE)
        val linkTask = binary.linkTask

        val reportsDir = buildDir.resolve(extension.buildDir).resolve(extension.reportsDir)
        val reportFile = reportsDir.resolve("${config.name}.json")

        executable = linkTask.outputFile.get().absolutePath
        args = listOf(reportFile.toString()) // TODO: configure!
        dependsOn(linkTask)
        doFirst {
            reportsDir.mkdirs()
            //standardOutput = FileOutputStream(reportFile)
            logger.lifecycle("Running benchmarks for ${config.name}")
        }
    }
}

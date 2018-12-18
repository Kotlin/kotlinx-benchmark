package org.jetbrains.gradle.benchmarks

import org.gradle.api.*
import org.gradle.api.artifacts.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.compile.*
import java.io.*

fun Project.createJvmBenchmarkCompileTask(
    extension: BenchmarksExtension,
    config: BenchmarkConfiguration,
    compileClasspath: FileCollection
) {
    val benchmarkBuildDir = benchmarkBuildDir(extension, config)
    task<JavaCompile>("${config.name}${BenchmarksPlugin.BENCHMARK_COMPILE_SUFFIX}") {
        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Compile JMH source files for '${config.name}'"
        dependsOn("${config.name}${BenchmarksPlugin.BENCHMARK_GENERATE_SUFFIX}")
        classpath = compileClasspath
        setSource(file("$benchmarkBuildDir/sources")) // TODO: try using FileTree since 4.0
        destinationDir = file("$benchmarkBuildDir/classes")
    }
}

fun createJmhGenerationRuntimeConfiguration(
    project: Project,
    config: BenchmarkConfiguration,
    classPath: FileCollection
): Configuration {
    // This configuration defines classpath for JMH generator, it should have everything available via reflection
    return project.configurations.create("${config.name}${BenchmarksPlugin.BENCHMARK_GENERATE_SUFFIX}").apply {
        isVisible = false
        description = "JMH Generator Runtime Configuration for '${config.name}'"

        @Suppress("UnstableApiUsage")
        defaultDependencies {
            it.add(project.dependencies.create("${BenchmarksPlugin.JMH_GENERATOR_DEPENDENCY}${config.jmhVersion}"))
            // TODO: runtimeClasspath or compileClasspath? how to avoid premature resolve()?
            it.add(project.dependencies.create(classPath))
        }
    }
}

fun Project.createJvmBenchmarkGenerateSourceTask(
    extension: BenchmarksExtension,
    config: BenchmarkConfiguration,
    classpath: Configuration,
    compilationTask: String,
    compilationOutput: FileCollection
) {
    val benchmarkBuildDir = benchmarkBuildDir(extension, config)
    task<JmhBytecodeGeneratorTask>("${config.name}${BenchmarksPlugin.BENCHMARK_GENERATE_SUFFIX}") {
        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Generate JMH source files for '${config.name}'"
        dependsOn(compilationTask)
        runtimeClasspath = classpath.resolve()
        inputClassesDirs = compilationOutput
        outputResourcesDir = file("$benchmarkBuildDir/resources")
        outputSourcesDir = file("$benchmarkBuildDir/sources")
    }
}

fun Project.createJvmBenchmarkExecTask(
    extension: BenchmarksExtension,
    config: BenchmarkConfiguration,
    runtimeClasspath: FileCollection
) {
    val benchmarkBuildDir = benchmarkBuildDir(extension, config)
    task<JavaExec>("${config.name}${BenchmarksPlugin.BENCHMARK_EXEC_SUFFIX}", depends = "benchmark") {
        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Execute benchmark for '${config.name}'"
        main = "org.openjdk.jmh.Main"
        classpath(
            file("$benchmarkBuildDir/classes"),
            file("$benchmarkBuildDir/resources"),
            runtimeClasspath
        )
        dependsOn("${config.name}${BenchmarksPlugin.BENCHMARK_COMPILE_SUFFIX}")
        tasks.getByName("benchmark").dependsOn(this)
    }
}

fun Project.benchmarkBuildDir(extension: BenchmarksExtension, config: BenchmarkConfiguration): File? {
    return file("$buildDir/${extension.buildDir}/${config.name}")
}


package org.jetbrains.gradle.benchmarks

import org.gradle.api.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*

fun Project.processJvmCompilation(config: JvmBenchmarkConfiguration) {
    project.logger.info("Configuring benchmarks for '${config.name}' using Kotlin/JVM")
    val compilation = config.compilation
    configureMultiplatformJvmCompilation(this, config, compilation)

    val workerClasspath = this.createJmhGenerationRuntimeConfiguration(config.name, config.jmhVersion)
    createJvmBenchmarkGenerateSourceTask(
        config,
        workerClasspath,
        compilation.compileDependencyFiles,
        compilation.compileAllTaskName,
        compilation.output.allOutputs
    )
    val runtimeClasspath = compilation.output.allOutputs + compilation.runtimeDependencyFiles
    createJvmBenchmarkCompileTask(config, runtimeClasspath)
    createJvmBenchmarkExecTask(config, runtimeClasspath)
}

private fun configureMultiplatformJvmCompilation(
    project: Project,
    config: JvmBenchmarkConfiguration,
    compilation: KotlinJvmCompilation
) {
    // Add JMH core library as an implementation dependency to the specified compilation
    val jmhCore = project.dependencies.create("${BenchmarksPlugin.JMH_CORE_DEPENDENCY}:${config.jmhVersion}")
    compilation.dependencies {
        implementation(jmhCore)
    }
}



package org.jetbrains.gradle.benchmarks

import org.gradle.api.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*

fun Project.processJvmCompilation(
    extension: BenchmarksExtension,
    config: BenchmarkConfiguration,
    compilation: KotlinJvmCompilation
) {
    configureMultiplatformJvmCompilation(this, config, compilation)

    val classpath = createJmhGenerationRuntimeConfiguration(this, config, compilation.runtimeDependencyFiles)

    createJvmBenchmarkGenerateSourceTask(
        extension,
        config,
        classpath,
        compilation.compileAllTaskName,
        compilation.output.allOutputs
    )
    val runtimeClasspath = compilation.output.allOutputs + compilation.runtimeDependencyFiles
    createJvmBenchmarkCompileTask(extension, config, runtimeClasspath)
    createJvmBenchmarkExecTask(extension, config, runtimeClasspath)
}

private fun configureMultiplatformJvmCompilation(
    project: Project,
    config: BenchmarkConfiguration,
    compilation: KotlinJvmCompilation
) {
    // Add JMH core library as an implementation dependency to the specified compilation
    val jmhCore = project.dependencies.create("${BenchmarksPlugin.JMH_CORE_DEPENDENCY}${config.jmhVersion}")
    compilation.dependencies {
        implementation(jmhCore)
    }
}



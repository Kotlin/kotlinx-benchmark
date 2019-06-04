package org.jetbrains.gradle.benchmarks

import org.gradle.api.*

fun Project.processJvmCompilation(target: KotlinJvmBenchmarkTarget) {
    project.logger.info("Configuring benchmarks for '${target.name}' using Kotlin/JVM")
    val compilation = target.compilation

    configureMultiplatformJvmCompilation(target)

    val workerClasspath = this.createJmhGenerationRuntimeConfiguration(target.name, target.jmhVersion)
    createJvmBenchmarkGenerateSourceTask(
        target,
        workerClasspath,
        compilation.compileDependencyFiles,
        compilation.compileAllTaskName,
        compilation.output.allOutputs
    )
    val runtimeClasspath = compilation.output.allOutputs + compilation.runtimeDependencyFiles
    createJvmBenchmarkCompileTask(target, runtimeClasspath)
    target.extension.configurations.forEach {
        createJvmBenchmarkExecTask(it, target, runtimeClasspath)
    }
}

private fun Project.configureMultiplatformJvmCompilation(target: KotlinJvmBenchmarkTarget) {
    // Add JMH core library as an implementation dependency to the specified compilation
    val jmhCore = dependencies.create("${BenchmarksPlugin.JMH_CORE_DEPENDENCY}:${target.jmhVersion}")

    // Add runtime library as an implementation dependency to the specified compilation
    val runtime = dependencies.create("${BenchmarksPlugin.RUNTIME_DEPENDENCY_BASE}-jvm:${target.extension.version}")

    target.compilation.dependencies {
        implementation(jmhCore)
        //implementation(runtime)
    }
}

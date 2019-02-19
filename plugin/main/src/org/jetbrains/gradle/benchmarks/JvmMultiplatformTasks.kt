package org.jetbrains.gradle.benchmarks

import org.gradle.api.*

fun Project.processJvmCompilation(config: JvmBenchmarkConfiguration) {
    project.logger.info("Configuring benchmarks for '${config.name}' using Kotlin/JVM")
    val compilation = config.compilation

    configureMultiplatformJvmCompilation(config)

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

private fun Project.configureMultiplatformJvmCompilation(config: JvmBenchmarkConfiguration) {
    // Add JMH core library as an implementation dependency to the specified compilation
    val jmhCore = dependencies.create("${BenchmarksPlugin.JMH_CORE_DEPENDENCY}:${config.jmhVersion}")

    // Add runtime library as an implementation dependency to the specified compilation
    val runtime = dependencies.create("${BenchmarksPlugin.RUNTIME_DEPENDENCY_BASE}-jvm:${config.extension.version}")

    config.compilation.dependencies {
        implementation(jmhCore)
        //implementation(runtime)
    }
}

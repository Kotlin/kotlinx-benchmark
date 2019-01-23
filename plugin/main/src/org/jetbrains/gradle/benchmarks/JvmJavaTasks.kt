package org.jetbrains.gradle.benchmarks

import org.gradle.api.*
import org.gradle.api.plugins.*

fun Project.processJavaSourceSet(config: JavaBenchmarkConfiguration) {
    project.logger.info("Configuring benchmarks for '${config.name}' using Java")

    val sourceSet = config.sourceSet

    // get configure source set and add JMH core dependency to it
    configureJmhDependency(this, config)

    // we need JMH generator runtime configuration for each BenchmarkConfiguration since version can be different
    val workerClasspath = createJmhGenerationRuntimeConfiguration(config.name, config.jmhVersion)

    // Create a task that will process output bytecode and generate benchmark Java source code
    createJvmBenchmarkGenerateSourceTask(
        config,
        workerClasspath,
        sourceSet.compileClasspath,
        sourceSet.classesTaskName,
        sourceSet.output
    )

    // Create a task that will compile generated Java source code into class files
    createJvmBenchmarkCompileTask(config, sourceSet.runtimeClasspath)

    // Create a task that will execute benchmark code
    createJvmBenchmarkExecTask(config, sourceSet.runtimeClasspath)
}

private fun configureJmhDependency(project: Project, config: JavaBenchmarkConfiguration) {
    val dependencies = project.dependencies

    // Add dependency to JMH core library to the source set designated by config.name
    val jmhCore = dependencies.create("${BenchmarksPlugin.JMH_CORE_DEPENDENCY}${config.jmhVersion}")
    val configurationRoot = "implementation"

    val dependencyConfiguration = if (config.name == "main")
        configurationRoot
    else
        "${config.name}${configurationRoot.capitalize()}"

    dependencies.add(dependencyConfiguration, jmhCore)
}

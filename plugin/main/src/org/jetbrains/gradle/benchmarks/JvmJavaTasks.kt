package org.jetbrains.gradle.benchmarks

import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.api.tasks.*

fun Project.configureJavaPlugin(
    extension: BenchmarksExtension,
    config: BenchmarkConfiguration
) {
    project.logger.info("Configuring benchmarks for '${config.name}' using Java")

    // get configure source set and add JMH core dependency to it
    val sourceSet = configureJavaSourceSet(this, config)

    // we need JMH generator runtime configuration for each BenchmarkConfiguration since version can be different
    val jmhRuntimeConfiguration = createJmhGenerationRuntimeConfiguration(
        this,
        config
    )

    // Create a task that will process output bytecode and generate benchmark Java source code
    createJvmBenchmarkGenerateSourceTask(
        extension,
        config,
        jmhRuntimeConfiguration,
        sourceSet.classesTaskName,
        sourceSet.output
    )

    // Create a task that will compile generated Java source code into class files
    createJvmBenchmarkCompileTask(extension, config, sourceSet.runtimeClasspath)

    // Create a task that will execute benchmark code
    createJvmBenchmarkExecTask(extension, config, sourceSet.runtimeClasspath)
}

private fun configureJavaSourceSet(project: Project, config: BenchmarkConfiguration): SourceSet {
    val dependencies = project.dependencies
    val javaConvention = project.convention.getPlugin(JavaPluginConvention::class.java)

    // Add dependency to JMH core library to the source set designated by config.name
    val jmhCore = dependencies.create("${BenchmarksPlugin.JMH_CORE_DEPENDENCY}${config.jmhVersion}")
    val configurationRoot = if (GRADLE_NEW) "implementation" else "compile"
    val dependencyConfiguration =
        if (config.name == "main") configurationRoot else "${config.name}${configurationRoot.capitalize()}"
    dependencies.add(dependencyConfiguration, jmhCore)
    return javaConvention.sourceSets.getByName(config.name)
}

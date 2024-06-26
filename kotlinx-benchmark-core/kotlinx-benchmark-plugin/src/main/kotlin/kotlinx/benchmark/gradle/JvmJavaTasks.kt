package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import org.gradle.api.*

@KotlinxBenchmarkPluginInternalApi
fun Project.processJavaSourceSet(target: JavaBenchmarkTarget) {
    logger.info("Configuring benchmarks for '${target.name}' using Java")

    val sourceSet = target.sourceSet

    // get configure source set and add JMH core dependency to it
    this.configureJmhDependency(target)

    // we need JMH generator runtime configuration for each BenchmarkConfiguration since version can be different
    val workerClasspath = createJmhGenerationRuntimeConfiguration(target.name, target.jmhVersion)

    // Create a task that will process output bytecode and generate benchmark Java source code
    createJvmBenchmarkGenerateSourceTask(
        target,
        workerClasspath,
        sourceSet.compileClasspath,
        sourceSet.classesTaskName,
        sourceSet.output
    )

    // Create a task that will compile generated Java source code into class files
    createJvmBenchmarkCompileTask(target, sourceSet.runtimeClasspath)

    // Create a task that will execute benchmark code
    target.extension.configurations.forEach {
        createJvmBenchmarkExecTask(it, target, sourceSet.runtimeClasspath)
    }
}

private fun Project.configureJmhDependency(target: JavaBenchmarkTarget) {
    val dependencies = dependencies

    // Add dependency to JMH core library to the source set designated by config.name
    val jmhCore = dependencies.create("${BenchmarksPlugin.JMH_CORE_DEPENDENCY}:${target.jmhVersion}")
    val configurationRoot = "implementation"

    val dependencyConfiguration = if (target.name == "main")
        configurationRoot
    else
        "${target.name}${configurationRoot.capitalize()}"

    dependencies.add(dependencyConfiguration, jmhCore)
}

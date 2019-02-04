package org.jetbrains.gradle.benchmarks

import org.gradle.api.*
import org.gradle.util.*
import org.jetbrains.kotlin.gradle.plugin.*

@Suppress("unused")
class BenchmarksPlugin : Plugin<Project> {
    companion object {
        const val PLUGIN_ID = "org.jetbrains.gradle.benchmarks.plugin"
        const val PLUGIN_VERSION = "0.1.6"

        const val BENCHMARKS_TASK_GROUP = "benchmark"
        const val BENCHMARK_EXTENSION_NAME = "benchmark"

        const val BENCHMARK_GENERATE_SUFFIX = "BenchmarkGenerate"
        const val BENCHMARK_COMPILE_SUFFIX = "BenchmarkCompile"
        const val BENCHMARK_EXEC_SUFFIX = "Benchmark"
        const val BENCHMARK_COMPILATION_NAME = "benchmark"
        const val BENCHMARK_DEPENDENCIES_SUFFIX = "BenchmarkDependencies"

        const val RUNTIME_DEPENDENCY_BASE = "org.jetbrains.gradle.benchmarks:runtime"
        const val JMH_CORE_DEPENDENCY = "org.openjdk.jmh:jmh-core"
        const val JMH_GENERATOR_DEPENDENCY = "org.openjdk.jmh:jmh-generator-bytecode:"

        const val RUN_BENCHMARKS_TASKNAME = "benchmark"
        const val ASSEMBLE_BENCHMARKS_TASKNAME = "assembleBenchmarks"
    }

    override fun apply(project: Project) = project.run {
        if (GradleVersion.current() < GradleVersion.version("4.10")) {
            logger.error("JetBrains Gradle Benchmarks plugin requires Gradle version 4.10 or higher")
            return // TODO: Do we need to fail build at this point or just ignore benchmarks?
        }

        val kotlinClass = tryGetClass<KotlinBasePluginWrapper>("org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper")
        if (kotlinClass != null) {
            plugins.findPlugin(kotlinClass)?.run {
                logger.info("Detected Kotlin plugin version '$kotlinPluginVersion'")
                if (VersionNumber.parse(kotlinPluginVersion) < VersionNumber(1, 3, 20, null))
                    logger.error("JetBrains Gradle Benchmarks plugin requires Kotlin version 1.3.20 or higher")
            }
        }

        // DO NOT use properties of an extension immediately, it will not contain any user-specified data
        val extension = extensions.create(BENCHMARK_EXTENSION_NAME, BenchmarksExtension::class.java, project)

        // Create empty task that will depend on all benchmark building tasks to build all benchmarks in a project
        val assembleBenchmarks = task<DefaultTask>(ASSEMBLE_BENCHMARKS_TASKNAME) {
            group = BENCHMARKS_TASK_GROUP
            description = "Generate and build all benchmarks in a project"
        }

        // Create empty task that will depend on all benchmark execution tasks to run all benchmarks in a project
        val runBenchmarks = task<DefaultTask>(RUN_BENCHMARKS_TASKNAME) {
            group = BENCHMARKS_TASK_GROUP
            description = "Execute all benchmarks in a project"
            extensions.extraProperties.set("idea.internal.test", System.getProperty("idea.active"))

            // Force all benchmarks runner to first build all benchmarks to ensure it won't spend time
            // running some benchmarks when other will fail to compile
            // Individual benchmarks depend on their respective building tasks for fast turnaround
            dependsOn(assembleBenchmarks)
        }

        // TODO: Design configuration avoidance
        // I currently don't how to do it correctly yet, so materialize all tasks after project evaluation. 
        afterEvaluate {
            processConfigurations(extension)
        }
    }
    
    private fun Project.processConfigurations(extension: BenchmarksExtension) {
        // Calling `all` on NDOC causes all items to materialize and be configured
        extension.configurations.all { config ->
            when (config) {
                is JavaBenchmarkConfiguration -> processJavaSourceSet(config)
                is JvmBenchmarkConfiguration -> processJvmCompilation(config)
                is JsBenchmarkConfiguration -> processJsCompilation(config)
                is NativeBenchmarkConfiguration -> processNativeCompilation(config)
            }
        }
    }
}

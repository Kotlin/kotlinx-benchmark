package org.jetbrains.gradle.benchmarks

import org.gradle.api.*
import org.gradle.util.*

@Suppress("unused")
class BenchmarksPlugin : Plugin<Project> {
    companion object {

        const val BENCHMARKS_TASK_GROUP = "benchmark"
        const val BENCHMARK_EXTENSION_NAME = "benchmark"

        const val BENCHMARK_GENERATE_SUFFIX = "BenchmarkGenerate"
        const val BENCHMARK_COMPILE_SUFFIX = "BenchmarkCompile"
        const val BENCHMARK_EXEC_SUFFIX = "Benchmark"
        const val BENCHMARK_COMPILATION_NAME = "benchmark"
        const val BENCHMARK_DEPENDENCIES_SUFFIX = "BenchmarkDependencies"

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

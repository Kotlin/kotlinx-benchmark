package org.jetbrains.gradle.benchmarks

import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.util.*
import org.jetbrains.kotlin.gradle.dsl.*

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

        const val JMH_CORE_DEPENDENCY = "org.openjdk.jmh:jmh-core:"
        const val JMH_GENERATOR_DEPENDENCY = "org.openjdk.jmh:jmh-generator-bytecode:"

        const val RUN_BENCHMARKS_TASKNAME = "benchmark"
        const val ASSEMBLE_BENCHMARKS_TASKNAME = "assembleBenchmarks"
    }

    override fun apply(project: Project) {
        if (GradleVersion.current() < GradleVersion.version("4.9")) {
            project.logger.error("JetBrains Gradle Benchmarks plugin requires Gradle version 4.9 or higher")
            return // TODO: Do we need to fail build at this point or just ignore benchmarks?
        }

        // DO NOT use properties of an extension immediately, it will not contain any user-specified data
        val extension = project.extensions.create(BENCHMARK_EXTENSION_NAME, BenchmarksExtension::class.java, project)

        // Create empty task to run all benchmarks in a project
        project.task<DefaultTask>(RUN_BENCHMARKS_TASKNAME) {
            group = BENCHMARKS_TASK_GROUP
            description = "Execute all benchmarks in a project"
        }

        // Create empty task to build all benchmarks in a project
        project.task<DefaultTask>(ASSEMBLE_BENCHMARKS_TASKNAME) {
            group = BENCHMARKS_TASK_GROUP
            description = "Generate and build all benchmarks in a project"
        }

        if (GRADLE_NEW) {
            configureBenchmarks(extension, project)
        } else {
            project.afterEvaluate {
                configureBenchmarks(extension, project)
            }
        }
    }

    private fun configureBenchmarks(extension: BenchmarksExtension, project: Project) {
        extension.configurations.all { config ->
            // This lambda is called as soon as configuration is added to a `benchmark` section
            // TODO: could be a problem, if it is configured before `kotlin` mpp section
            val mpp = project.extensions.findByType(KotlinMultiplatformExtension::class.java)
            when {
                mpp != null -> {
                    project.configureKotlinMultiplatform(extension, config, mpp)
                }
                else -> {
                    project.plugins.withType(JavaPlugin::class.java) {
                        project.configureJavaPlugin(extension, config)
                    }
                }
            }
        }
    }
}

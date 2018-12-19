package org.jetbrains.gradle.benchmarks

import org.gradle.api.*
import org.gradle.api.plugins.*
import org.jetbrains.kotlin.gradle.dsl.*

@Suppress("unused")
class BenchmarksPlugin : Plugin<Project> {
    companion object {

        const val BENCHMARKS_TASK_GROUP = "benchmark"
        const val BENCHMARK_EXTENSION_NAME = "benchmark"

        const val BENCHMARK_GENERATE_SUFFIX = "BenchmarkGenerate"
        const val BENCHMARK_COMPILE_SUFFIX = "BenchmarkCompile"
        const val BENCHMARK_EXEC_SUFFIX = "Benchmark"
        const val BENCHMARK_DEPENDENCIES_SUFFIX = "BenchmarkDependencies"

        const val JMH_CORE_DEPENDENCY = "org.openjdk.jmh:jmh-core:"
        const val JMH_GENERATOR_DEPENDENCY = "org.openjdk.jmh:jmh-generator-bytecode:"
    }

    override fun apply(project: Project) {
        // DO NOT use properties of an extension immediately, it will not contain any user-specified data
        val extension = project.extensions.create(BENCHMARK_EXTENSION_NAME, BenchmarksExtension::class.java, project)

        // Create empty task to serve as a root for all benchmarks in a project
        project.task<DefaultTask>("benchmark") {
            group = BENCHMARKS_TASK_GROUP
            description = "Execute all benchmarks in a project"
        }

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

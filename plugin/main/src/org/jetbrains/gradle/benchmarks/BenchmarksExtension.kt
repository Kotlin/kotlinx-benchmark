package org.jetbrains.gradle.benchmarks

import groovy.lang.*
import org.gradle.api.*
import org.gradle.api.plugins.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*

open class BenchmarksExtension(val project: Project) {
    var buildDir: String = "benchmarks"
    var reportsDir: String = "reports"

    internal val multiplatform = project.extensions.findByType(KotlinMultiplatformExtension::class.java)
    internal val javaConvention = project.convention.findPlugin(JavaPluginConvention::class.java)

    fun configurations(configureClosure: Closure<NamedDomainObjectContainer<BenchmarkConfiguration>>): NamedDomainObjectContainer<BenchmarkConfiguration> {
        return configurations.configure(configureClosure)
    }

    val configurations: NamedDomainObjectContainer<BenchmarkConfiguration> = run {
        project.container(BenchmarkConfiguration::class.java) { name ->
            when {
                multiplatform != null -> {
                    val compilations = multiplatform.targets.flatMap { it.compilations }
                    val compilation = compilations.singleOrNull { it.apiConfigurationName.removeSuffix("Api") == name }
                    when (compilation) {
                        null -> {
                            project.logger.warn("Warning: Cannot find a benchmark compilation '$name', ignoring.")
                            BenchmarkConfiguration(this, name) // ignore
                        }
                        is KotlinJvmCompilation -> {
                            JvmBenchmarkConfiguration(this, name, compilation)
                        }
                        is KotlinJsCompilation -> {
                            JsBenchmarkConfiguration(this, name, compilation)
                        }
                        is KotlinNativeCompilation -> {
                            NativeBenchmarkConfiguration(this, name, compilation)
                        }
                        else -> {
                            project.logger.warn("Warning: Unsupported compilation '$compilation', ignoring.")
                            BenchmarkConfiguration(this, name) // ignore
                        }
                    }

                }
                javaConvention != null -> {
                    val sourceSet = javaConvention.sourceSets.findByName(name)
                    when (sourceSet) {
                        null -> {
                            project.logger.warn("Warning: Cannot find a benchmark sourceSet '$name', ignoring.")
                            BenchmarkConfiguration(this, name) // ignore
                        }
                        else -> {
                            JavaBenchmarkConfiguration(this, name, sourceSet)
                        }
                    }
                }
                else -> {
                    project.logger.warn("Warning: No Java or Kotlin Multiplatform plugin found, ignoring.")
                    BenchmarkConfiguration(this, name) // ignore
                }
            }
        }
    }
}

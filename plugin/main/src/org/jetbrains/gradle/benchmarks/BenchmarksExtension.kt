package org.jetbrains.gradle.benchmarks

import groovy.lang.*
import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.util.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*

fun Project.benchmark(configure: Action<BenchmarksExtension>) {
    configure.execute(extensions.getByType(BenchmarksExtension::class.java))
}

open class BenchmarksExtension(val project: Project) {
    var buildDir: String = "benchmarks"
    var reportsDir: String = "reports/benchmarks"

    val version = BenchmarksPlugin.PLUGIN_VERSION

    val defaults = BenchmarkConfigurationDefaults()
    
    fun defaults(configure: Action<BenchmarkConfigurationDefaults>) {
        configure.execute(defaults)
    }
    
    fun defaults(configure: Closure<BenchmarkConfigurationDefaults>) {
        ConfigureUtil.configureSelf(configure, defaults)
    }

    fun configurations(configureClosure: Closure<NamedDomainObjectContainer<BenchmarkConfiguration>>): NamedDomainObjectContainer<BenchmarkConfiguration> {
        return configurations.configure(configureClosure)
    }

    val configurations: NamedDomainObjectContainer<BenchmarkConfiguration> = run {
        project.container(BenchmarkConfiguration::class.java) { name ->
            val multiplatformClass = tryGetClass<KotlinMultiplatformExtension>("org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension")
            val multiplatform = multiplatformClass?.let { project.extensions.findByType(it) }
            val javaConvention = project.convention.findPlugin(JavaPluginConvention::class.java)

            // Factory function which is called when a given registration is materialized
            // Subscribing to NDOC (configurations.all) will cause every registration to eagerly materialize
            // Materialization includes calling this factory method AND calling user-provided configuration closure
            // We need to know type of the compilation/sourceSet for the given name to provide proper typed object
            // to user configuration script.  

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

package kotlinx.benchmark.gradle

import groovy.lang.*
import org.gradle.api.*
import org.gradle.api.plugins.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*

fun Project.benchmark(configure: Action<BenchmarksExtension>) {
    configure.execute(extensions.getByType(BenchmarksExtension::class.java))
}

open class BenchmarksExtension(val project: Project) {
    var buildDir: String = "benchmarks"
    var reportsDir: String = "reports/benchmarks"
    var benchsDescriptionDir: String = "benchsDescription"

    val version = BenchmarksPlugin.PLUGIN_VERSION

    fun configurations(configureClosure: Closure<NamedDomainObjectContainer<BenchmarkConfiguration>>): NamedDomainObjectContainer<BenchmarkConfiguration> {
        return configurations.configure(configureClosure)
    }

    val configurations: NamedDomainObjectContainer<BenchmarkConfiguration> = run {
        val container = project.container(BenchmarkConfiguration::class.java) { name ->
            BenchmarkConfiguration(this, name)
        }
        container.register("main")
        container
    }
    
    fun targets(configureClosure: Closure<NamedDomainObjectContainer<BenchmarkTarget>>): NamedDomainObjectContainer<BenchmarkTarget> {
        return targets.configure(configureClosure)
    }

    val targets: NamedDomainObjectContainer<BenchmarkTarget> = run {
        project.container(BenchmarkTarget::class.java) { name ->
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
                            BenchmarkTarget(this, name) // ignore
                        }
                        is KotlinJvmCompilation -> {
                            KotlinJvmBenchmarkTarget(this, name, compilation)
                        }
                        is KotlinJsCompilation -> {
                            JsBenchmarkTarget(this, name, compilation)
                        }
                        is KotlinNativeCompilation -> {
                            NativeBenchmarkTarget(this, name, compilation)
                        }
                        else -> {
                            project.logger.warn("Warning: Unsupported compilation '$compilation', ignoring.")
                            BenchmarkTarget(this, name) // ignore
                        }
                    }

                }
                javaConvention != null -> {
                    val sourceSet = javaConvention.sourceSets.findByName(name)
                    when (sourceSet) {
                        null -> {
                            project.logger.warn("Warning: Cannot find a benchmark sourceSet '$name', ignoring.")
                            BenchmarkTarget(this, name) // ignore
                        }
                        else -> {
                            JavaBenchmarkTarget(this, name, sourceSet)
                        }
                    }
                }
                else -> {
                    project.logger.warn("Warning: No Java or Kotlin Multiplatform plugin found, ignoring.")
                    BenchmarkTarget(this, name) // ignore
                }
            }
        }
    }
}

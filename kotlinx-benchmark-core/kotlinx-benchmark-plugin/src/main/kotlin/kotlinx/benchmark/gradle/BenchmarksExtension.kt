package kotlinx.benchmark.gradle

import groovy.lang.Closure
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import org.gradle.api.*
import org.gradle.api.plugins.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation

fun Project.benchmark(configure: Action<BenchmarksExtension>) {
    extensions.configure(BenchmarksExtension::class.java, configure)
}

open class BenchmarksExtension
@KotlinxBenchmarkPluginInternalApi
constructor(
    val project: Project
) {

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
            val javaExtension = project.extensions.findByType(JavaPluginExtension::class.java)

            // Factory function which is called when a given registration is materialized
            // Subscribing to NDOC (configurations.all) will cause every registration to eagerly materialize
            // Materialization includes calling this factory method AND calling user-provided configuration closure
            // We need to know type of the compilation/sourceSet for the given name to provide proper typed object
            // to user configuration script.

            when {
                multiplatform != null -> {
                    val target = multiplatform.targets.findByName(name)
                    // We allow the name to be either a target or a source set
                    when (val compilation = target?.compilations?.findByName(KotlinCompilation.MAIN_COMPILATION_NAME)
                        ?: multiplatform.targets.flatMap { it.compilations }
                            .find { it.defaultSourceSet.name == name }) {
                        null -> {
                            project.logger.warn("Warning: Cannot find a benchmark compilation '$name', ignoring.")
                            BenchmarkTarget(this, name) // ignore
                        }

                        is KotlinJvmCompilation -> {
                            KotlinJvmBenchmarkTarget(this, name, compilation)
                        }

                        is KotlinJsCompilation -> {
                            check(compilation is KotlinJsIrCompilation) {
                                "Legacy Kotlin/JS backend is not supported. Please migrate to the Kotlin/JS IR compiler backend."
                            }
                            if (compilation.target.platformType != KotlinPlatformType.wasm) {
                                JsBenchmarkTarget(this, name, compilation)
                            } else {
                                WasmBenchmarkTarget(this, name, compilation)
                            }
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

                javaExtension != null -> {
                    when (val sourceSet = javaExtension.sourceSets.findByName(name)) {
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

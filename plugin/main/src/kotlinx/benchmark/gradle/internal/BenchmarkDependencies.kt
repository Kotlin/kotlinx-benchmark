package kotlinx.benchmark.gradle.internal

import kotlinx.benchmark.gradle.BenchmarksExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.Usage.JAVA_RUNTIME
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.model.ObjectFactory

/**
 * Utility for containing all Gradle [Configuration]s used by Benchmarks.
 */
internal class BenchmarkDependencies(
    project: Project,
    benchmarksExtension: BenchmarksExtension,
) {
    private val objects: ObjectFactory = project.objects

    private val benchmarkGenerator: Configuration =
        project.configurations.create("benchmarkGenerator") {
            it.description =
                "Internal kotlinx-benchmark Configuration. Contains declared dependencies required for running benchmark generators."
            it.declarable()

            it.defaultDependencies { deps ->
                deps.add(project.dependencies.create("com.squareup:kotlinpoet:${BenchmarksPluginConstants.KOTLIN_POET_VERSION}"))

                deps.addAllLater(
                    benchmarksExtension.project.providers.gradleProperty("benchmarks_ksp_version").orElse(
                        BenchmarksPluginConstants.DEFAULT_KSP_VERSION
                    )
                        .map {
                            listOf(
                                project.dependencies.create("com.google.devtools.ksp:symbol-processing-aa-embeddable:$it"),
                                project.dependencies.create("com.google.devtools.ksp:symbol-processing-api:$it"),
                                project.dependencies.create("com.google.devtools.ksp:symbol-processing-common-deps:$it"),
                            )
                        }
                )
            }
        }

    val benchmarkGeneratorResolver: Configuration =
        project.configurations.create("benchmarkGenerator.resolver") {
            // The name has a dot, to prevent Gradle from generating a Kotlin DSL accessor.
            it.description =
                "Internal kotlinx-benchmark Configuration. Resolves dependencies required for running benchmark generators."
            it.resolvable()

            it.extendsFrom(benchmarkGenerator)

            it.attributes { atts ->
                atts.attribute(USAGE_ATTRIBUTE, objects.named(Usage::class.java, JAVA_RUNTIME))
            }
        }

    internal companion object {
        /** Mark this [Configuration] as one that will be used to declare dependencies. */
        private fun Configuration.declarable() {
            isCanBeDeclaredCompat = true
            isCanBeResolved = false
            isCanBeConsumed = false
            isVisibleCompat = false
        }

        /** Mark this [Configuration] as one that will be used to resolve dependencies. */
        private fun Configuration.resolvable() {
            isCanBeDeclaredCompat = false
            isCanBeResolved = true
            isCanBeConsumed = false
            isVisibleCompat = false
        }
    }
}

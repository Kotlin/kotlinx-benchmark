package kotlinx.benchmark.gradle.internal

import kotlinx.benchmark.gradle.BenchmarksExtension
import org.gradle.api.*
import org.gradle.api.artifacts.*
import org.gradle.api.attributes.*
import org.gradle.api.attributes.Usage.*
import org.gradle.api.model.*
import org.gradle.util.GradleVersion

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
                deps.addLater(
                    benchmarksExtension.kotlinCompilerVersion.map { version ->
                        project.dependencies.create("org.jetbrains.kotlin:kotlin-compiler-embeddable:$version")
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

        @Suppress("UnstableApiUsage")
        /** `true` if [Configuration.isCanBeDeclared] is supported by the current Gradle version. */
        private val isCanBeDeclaredSupported = GradleVersion.current() >= GradleVersion.version("8.2")

        // Remove when minimum supported Gradle version is >= 8.2
        @Suppress("UnstableApiUsage")
        private var Configuration.isCanBeDeclaredCompat: Boolean
            get() = if (isCanBeDeclaredSupported) isCanBeDeclared else false
            set(value) {
                if (isCanBeDeclaredSupported) isCanBeDeclared = value
            }

        /** `true` is [Configuration.isVisible] is still available in the current Gradle version */
        private val isVisibleSupported = GradleVersion.current() < GradleVersion.version("9.0")

        private var Configuration.isVisibleCompat: Boolean
            get() = if (isVisibleSupported) isVisible else false
            set(value) {
                if (isVisibleSupported) isVisible = value
            }
    }
}

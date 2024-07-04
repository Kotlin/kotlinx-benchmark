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
                "Internal Kotlinx Benchmarks Configuration. Contains declared dependencies required for running benchmark generators."
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
                "Internal Kotlinx Benchmarks Configuration. Resolves dependencies required for running benchmark generators."
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
            isVisible = false
        }

        /** Mark this [Configuration] as one that will be used to resolve dependencies. */
        private fun Configuration.resolvable() {
            isCanBeDeclaredCompat = false
            isCanBeResolved = true
            isCanBeConsumed = false
            isVisible = false
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
    }
}

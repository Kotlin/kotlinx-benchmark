package kotlinx.benchmarks_build.dev_maven_publish

import org.gradle.api.file.*
import org.gradle.api.provider.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.*
import org.gradle.api.tasks.testing.*
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.process.JavaForkOptions
import java.io.File

abstract class DevMavenPublishExtension(
    /**
     * Resolves Dev Maven repos from the current project's `devPublication` dependencies.
     *
     * Must only contain directories.
     */
    private val devMavenRepositories: FileCollection,
) {

    /**
     * Files suitable for registering as a task input (as in, the files are reproducible-build compatible).
     */
    private val devMavenRepositoriesInputFiles: Provider<List<File>> =
        devMavenRepositories
            // Convert to a FileTree, which converts directories to all files, so we can filter on specific files.
            .asFileTree
            // Exclude Maven Metadata files because they contain timestamps, meaning tasks that use
            // devMavenRepositories as an input will never be up-to-date.
            // The Gradle Module Metadata contains the same information (and more),
            // so the Maven metadata is redundant.
            .matching { exclude("**/maven-metadata*.xml") }
            // FileTrees have an unstable order (even on the same machine), which means Gradle up-to-date checks fail.
            // So, manually sort the files so that Gradle can cache the task.
            .elements
            .map { files -> files.map { it.asFile }.sorted() }

    /**
     * Configures [Test] task to register [devMavenRepositories] as a task input,
     * and (if possible) adds `devMavenRepositories` as a [JavaForkOptions.systemProperty].
     */
    fun configureTask(task: Test) {
        task.inputs.files(devMavenRepositoriesInputFiles)
            .withPropertyName("devMavenPublish.devMavenRepositoriesInputFiles")
            .withPathSensitivity(RELATIVE)

        task.dependsOn(devMavenRepositories)
        task.jvmArgumentProviders.add(
            SystemPropertyArgumentProvider(
                key = "devMavenRepositories",
                value = devMavenRepositories.elements.map { paths ->
                    paths.joinToString(",") { it.asFile.canonicalFile.invariantSeparatorsPath }
                },
                transformer = { it.orNull },
            )
        )
    }

    companion object {
        const val DEV_MAVEN_PUBLISH_EXTENSION_NAME = "devMavenPublish"
    }
}

/**
 * Provide a Java system property.
 *
 * [value] is not registered as a Gradle Task input.
 */
private class SystemPropertyArgumentProvider<T : Any>(
    @get:Input
    val key: String,
    private val value: T,
    private val transformer: (value: T) -> String?,
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> {
        val value = transformer(value) ?: return emptyList()
        return listOf("-D$key=$value")
    }
}

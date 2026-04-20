package kotlinx.benchmark.gradle

import org.gradle.api.*
import org.gradle.api.artifacts.type.*
import org.gradle.api.file.*
import org.gradle.api.provider.*
import java.io.*
import java.net.*

/**
 * File with helper methods for creating the necessary plumbing for us to be able to generate and run benchmarks
 * on Android using Jetpack Microbenchmark.
 *
 * Unlike other targets, this requires a standalone Gradle project, which will be created and managed under the hood
 * without the user needing to know the specifics.
 */

// Setup Gradle tasks needed to create and run benchmarks on Android
internal fun Project.processAndroidCompilation(config: AndroidBenchmarkTarget) {
    project.logger.info("Configuring benchmarks for '${config.name}'")
    createUnpackLibraryArtifactTask(config, config.compilationName)
    createSetupAndroidProjectTask(config)
    createAndroidBenchmarkGenerateSourceTask(config, config.compilationName)
    createAndroidBenchmarkExecTask(config)
}

private fun Project.createUnpackLibraryArtifactTask(target: AndroidBenchmarkTarget, compilationName: String) {
    task<AndroidUnpackLibraryArtifactTask>(generateUnpackArtifactTaskName(target)) {
        group = "benchmark"
        description = "Unpacks the AAR or JAR file produced by compilation '$compilationName'"
        val (buildTaskName, libFile) = resolveLibraryArtifact(target)
        dependsOn(buildTaskName)
        this.libraryFile.set(libFile)
        this.unpackedAarDir.set(getUnpackAarDir(compilationName))
    }
}

private fun Project.createSetupAndroidProjectTask(target: AndroidBenchmarkTarget) {
    val pluginJarFile = project.file(
        URLDecoder.decode(
            BenchmarksPlugin::class.java.protectionDomain.codeSource.location.path,
            "UTF-8"
        )
    )

    task<AndroidSetupBenchmarkProject>(generateSetupProjectTaskName(target)) {
        group = "benchmark"
        description = "Sets up Android project to generate benchmarks into"
        val (buildTaskName, libFile) = resolveLibraryArtifact(target)
        dependsOn(buildTaskName)
        configuration.set(target.createTemplateConfig())
        libraryFile.set(libFile)
        pluginJar.set(pluginJarFile)
        dependencies.from(collectProjectTransitiveDependencies(target))
        outputDir.set(benchmarkBuildDir(target))
    }
}

private fun Project.createAndroidBenchmarkGenerateSourceTask(target: AndroidBenchmarkTarget, compilationName: String) {
    val buildDir = benchmarkBuildDir(target)
    val targetName = target.name
    val unpackedDir = getUnpackAarDir(compilationName)

    task<AndroidGeneratorTask>(generateSourcesTaskName(target)) {
        group = "benchmark"
        description = "Generates benchmark source files for `${targetName}`"
        dependsOn(generateUnpackArtifactTaskName(target))
        dependsOn(generateSetupProjectTaskName(target))
        this.unpackedAarDir.set(unpackedDir)
        this.compilationName.set(compilationName)
        this.targetName.set(targetName)
        this.benchmarkProjectDir.set(buildDir.resolve("microbenchmark/src/androidTest/kotlin"))
    }
}

private fun Project.createAndroidBenchmarkExecTask(target: AndroidBenchmarkTarget) {
    val buildDir = benchmarkBuildDir(target)
    val deviceOutputDir = buildDir.resolve("microbenchmark/build/${target.deviceResultOutputDirectory}")
    val benchmarkResultsDir = layout.buildDirectory.map {
        it.dir("${target.extension.reportsDir}/${target.name}")
    }
    task<AndroidExecTask>(generateExecTaskName(target)) {
        group = "benchmark"
        description = "Executes benchmarks for `${target.name}`"
        dependsOn(generateSourcesTaskName(target))
        this.adb.set(target.adb)
        this.timeoutMs.set(target.timeout.inWholeMilliseconds)
        this.benchmarkProjectDir.set(buildDir)
        this.deviceOutputDir.set(deviceOutputDir)
        this.benchmarkResultsDir.set(benchmarkResultsDir)
        this.dryRun.set(target.dryRun)
    }
}

private fun generateUnpackArtifactTaskName(target: AndroidBenchmarkTarget): String {
    return "unpack${target.gradleTaskName}Artifact"
}

private fun generateSetupProjectTaskName(target: AndroidBenchmarkTarget): String {
    // As long as AGP doesn't support custom compilations, we ignore the configuration name and just uses the target name,
    // E.g., instead of `setupAndroidMainBenchmarkProject` it just becomes `setupAndroidBenchmarkProject`.
    return "setup${target.name.replaceFirstChar { it.uppercase() }}BenchmarkProject"
}

private fun generateExecTaskName(target: AndroidBenchmarkTarget): String {
    // As long as AGP doesn't support custom compilations, we ignore the configuration name and just uses the target name,
    // E.g., instead of `androidMainBenchmark` it just becomes `androidBenchmark`.
    return "${target.name}${BenchmarksPlugin.BENCHMARK_EXEC_SUFFIX}"
}

private fun generateSourcesTaskName(target: AndroidBenchmarkTarget): String {
    // As long as AGP doesn't support custom compilations, we ignore the configuration name and just uses the target name,
    // E.g., instead of `androidMainBenchmarkGenerate` it just becomes `androidBenchmarkGenerate`.
    return "${target.name}${BenchmarksPlugin.BENCHMARK_GENERATE_SUFFIX}"
}

/**
 * Returns the task name that produces the library artifact and the path to that artifact.
 *
 * Two scenarios are considered:
 * 1. The main project supports Anroid and produces an AAR. This is done through the `bundle*Aar` task.
 * 2. The main project supports Android indirectly through the JVM target. This is done through the `assemble*` task.
 */
private fun Project.resolveLibraryArtifact(target: AndroidBenchmarkTarget): Pair<String, Provider<RegularFile>> {
    val bundleAarTaskName = "bundle${target.gradleTaskName}Aar"
    return if (tasks.names.contains(bundleAarTaskName)) {
        bundleAarTaskName to layout.buildDirectory.file("outputs/aar/${project.name}.aar")
    } else {
        "assemble${target.gradleTaskName}" to layout.buildDirectory.file("libs/${project.name}.jar")
    }
}

private fun Project.getUnpackAarDir(compilationName: String): File {
    return File("${project.projectDir}/build/outputs/unpacked-aar/$compilationName")
}

// Collect all transitive runtime JAR and AAR dependencies from the Android runtime classpath.
// These must be added manually to the generated project.
private fun Task.collectProjectTransitiveDependencies(target: AndroidBenchmarkTarget): FileCollection? {
    val runtimeClasspathConfig = project.configurations.findByName("${target.name}RuntimeClasspath")
    if (runtimeClasspathConfig != null) {
        val jarFiles = artifactFilesOfType(runtimeClasspathConfig, ArtifactTypeDefinition.JAR_TYPE)
        val aarFiles = artifactFilesOfType(runtimeClasspathConfig, "aar")

        // We need to filter out dependencies provided by the template project itself, which is the Kotlin Stdlib and
        // its transitive dependencies as well as the kotlinx-benchmark runtime.
        val stdlibConfig = project.configurations.detachedConfiguration(
            project.dependencies.create("org.jetbrains.kotlin:kotlin-stdlib:${target.kotlinVersion}")
        )
        val stdlibTransitiveDependencies: Set<File> = artifactFilesOfType(stdlibConfig, ArtifactTypeDefinition.JAR_TYPE).files
        return project.files(jarFiles, aarFiles)
            .filter { it !in stdlibTransitiveDependencies }
            .filter { it.name != "kotlinx-benchmark-runtime.aar" }
    } else {
        logger.warn(
            "Could not find runtime classpath configuration for Android target '${target.name}'. " +
                    "Transitive dependencies will not be included in the Android benchmark project."
        )
        return null
    }
}

// Helper to filter all artifacts by a given file type
private fun artifactFilesOfType(config: org.gradle.api.artifacts.Configuration, type: String) =
    config.incoming.artifactView { view ->
        view.lenient(true)
        view.attributes { attrs ->
            attrs.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, type)
        }
    }.artifacts.artifactFiles

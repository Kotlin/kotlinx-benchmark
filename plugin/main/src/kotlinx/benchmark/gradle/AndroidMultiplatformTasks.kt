package kotlinx.benchmark.gradle

import org.gradle.api.*
import org.gradle.api.artifacts.type.*
import org.gradle.api.file.*
import org.gradle.api.provider.*
import java.io.*
import java.net.*
import kotlin.time.Duration.Companion.minutes

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
    // Android doesn't support custom compilations, so we share the same compilation across all benchmark configurations
    createUnpackLibraryArtifactTask(config)
    config.extension.configurations.forEach { benchmarkConfig ->
        createSetupAndroidProjectTask(config, benchmarkConfig)
        createAndroidBenchmarkGenerateSourceTask(config, benchmarkConfig)
        createAndroidBenchmarkExecTask(config, benchmarkConfig)
    }
    createDeviceLockingTasks(config)
}

private fun Project.createUnpackLibraryArtifactTask(target: AndroidBenchmarkTarget) {
    val compilationName = target.compilationName
    task<AndroidUnpackLibraryArtifactTask>(generateUnpackArtifactTaskName(target)) {
        group = "benchmark"
        description = "Unpacks the AAR or JAR file produced by compilation '$compilationName}'"
        val (buildTaskName, libFile) = resolveLibraryArtifact(target)
        dependsOn(buildTaskName)
        this.libraryFile.set(libFile)
        this.unpackedAarDir.set(getUnpackAarDir(compilationName))
    }
}

private fun Project.createSetupAndroidProjectTask(target: AndroidBenchmarkTarget, config: BenchmarkConfiguration) {
    val pluginJarFile = project.file(
        URLDecoder.decode(
            BenchmarksPlugin::class.java.protectionDomain.codeSource.location.path,
            "UTF-8"
        )
    )

    task<AndroidSetupBenchmarkProject>(generateSetupProjectTaskName(target, config)) {
        group = "benchmark"
        description = "Sets up Android project to generate benchmarks into"
        val (buildTaskName, libFile) = resolveLibraryArtifact(target)
        dependsOn(buildTaskName)
        configuration.set(target.createTemplateConfig())
        libraryFile.set(libFile)
        pluginJar.set(pluginJarFile)
        dependencies.from(collectProjectTransitiveDependencies(target))
        outputDir.set(androidBenchmarkBuildDir(target, config))
    }
}

private fun Project.createAndroidBenchmarkGenerateSourceTask(
    target: AndroidBenchmarkTarget,
    config: BenchmarkConfiguration
) {
    val buildDir = androidBenchmarkBuildDir(target, config)
    val targetName = target.name
    val unpackedDir = getUnpackAarDir(target.compilationName)
    val paramOverrides = config.params.mapValues { (_, values) ->
        // `null` cannot be represented as string, but will turn into "null", which is mirroring the JHM behavior
        values.map { it.toString() }
    }

    task<AndroidGeneratorTask>(generateSourcesTaskName(target, config)) {
        group = "benchmark"
        description = "Generates benchmark source files for `${targetName}`"
        dependsOn(generateUnpackArtifactTaskName(target))
        dependsOn(generateSetupProjectTaskName(target, config))
        this.unpackedAarDir.set(unpackedDir)
        this.compilationName.set(config.name)
        this.targetName.set(targetName)
        this.benchmarkProjectDir.set(buildDir.resolve("microbenchmark/src/androidTest/kotlin"))
        this.includePatterns.set(config.includes)
        this.excludePatterns.set(config.excludes)
        this.paramOverrides.set(paramOverrides)
    }
}

private fun Project.createAndroidBenchmarkExecTask(target: AndroidBenchmarkTarget, config: BenchmarkConfiguration) {
    val buildDir = androidBenchmarkBuildDir(target, config)
    val deviceOutputDir = buildDir.resolve("microbenchmark/build/${target.deviceResultOutputDirectory}")
    val benchmarkResultsDir = layout.buildDirectory.map {
        it.dir("${target.extension.reportsDir}/${target.name}${config.capitalizedName()}")
    }
    task<AndroidExecTask>(generateExecTaskName(target, config)) {
        group = "benchmark"
        description = "Executes benchmarks for `${target.name}`"
        dependsOn(generateSourcesTaskName(target, config))
        this.adb.set(target.adb)
        this.timeoutMs.set(target.timeout.inWholeMilliseconds)
        this.benchmarkProjectDir.set(buildDir)
        this.deviceOutputDir.set(deviceOutputDir)
        this.benchmarkResultsDir.set(benchmarkResultsDir)
        this.dryRun.set(target.dryRun)
        this.reportFormat.set(config.reportFormat ?: "text")
    }
}

// Pass-through `lockClocks` and `unlockClocks` to the generated benchmark project.
// These are not configuration-specific — they use the "main" (first) configuration's project.
private fun Project.createDeviceLockingTasks(target: AndroidBenchmarkTarget) {
    val mainConfig = target.extension.configurations.first()
    task<ExecBenchmarkProjectTask>("lockClocks") {
        group = "benchmark"
        description = "Locks clocks of connected, supported, rooted Android device."
        dependsOn(generateSourcesTaskName(target, mainConfig))
        args.set(listOf("lockClocks"))
        timeoutMs.set(1.minutes.inWholeMilliseconds)
        benchmarkProjectDir.set(androidBenchmarkBuildDir(target, mainConfig))
    }
    task<ExecBenchmarkProjectTask>("unlockClocks") {
        group = "benchmark"
        description = "Unlocks clocks of Android device by rebooting"
        dependsOn(generateSourcesTaskName(target, mainConfig))
        args.set(listOf("unlockClocks"))
        timeoutMs.set(1.minutes.inWholeMilliseconds)
        benchmarkProjectDir.set(androidBenchmarkBuildDir(target, mainConfig))
    }
}

private fun generateUnpackArtifactTaskName(target: AndroidBenchmarkTarget): String {
    return "unpack${target.gradleTaskName}Artifact"
}

private fun generateSetupProjectTaskName(target: AndroidBenchmarkTarget, config: BenchmarkConfiguration): String {
    return "setup${target.name.replaceFirstChar { it.uppercase() }}${config.capitalizedName()}BenchmarkProject"
}

private fun generateExecTaskName(target: AndroidBenchmarkTarget, config: BenchmarkConfiguration): String {
    return "${target.name}${config.capitalizedName()}${BenchmarksPlugin.BENCHMARK_EXEC_SUFFIX}"
}

private fun generateSourcesTaskName(target: AndroidBenchmarkTarget, config: BenchmarkConfiguration): String {
    return "${target.name}${config.capitalizedName()}${BenchmarksPlugin.BENCHMARK_GENERATE_SUFFIX}"
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

// Returns a config-specific directory for the Android benchmark project.
private fun Project.androidBenchmarkBuildDir(target: AndroidBenchmarkTarget, config: BenchmarkConfiguration): File {
    val suffix = "-${config.name}"
    return layout.buildDirectory.dir("${target.extension.buildDir}/${target.name}$suffix")
        .get().asFile
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
            .filter { "kotlinx-benchmark-runtime" !in it.path }
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

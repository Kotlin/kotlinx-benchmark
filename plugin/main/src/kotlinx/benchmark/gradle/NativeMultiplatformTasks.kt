package kotlinx.benchmark.gradle

import org.gradle.api.*
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.konan.target.*
import java.io.*

fun Project.processNativeCompilation(target: NativeBenchmarkTarget) {
    val compilation = target.compilation
    if (compilation.target.konanTarget != HostManager.host) {
        project.logger.warn("Skipping benchmarks for '${target.name}' because they cannot be run on current OS")
        return        
    }
    
    project.logger.info("Configuring benchmarks for '${target.name}' using Kotlin/Native")
    
    configureMultiplatformNativeCompilation(target, compilation)

    createNativeBenchmarkGenerateSourceTask(target)

    val benchmarkCompilation = createNativeBenchmarkCompileTask(target)
    target.extension.configurations.forEach {
        createNativeBenchmarkExecTask(it, target, benchmarkCompilation)
    }
}

private fun generateSourceTaskName(target: NativeBenchmarkTarget)
        = target.name + BenchmarksPlugin.BENCHMARK_GENERATE_SUFFIX

private fun Project.createNativeBenchmarkGenerateSourceTask(target: NativeBenchmarkTarget) {
    val benchmarkBuildDir = benchmarkBuildDir(target)
    task<NativeSourceGeneratorTask>(generateSourceTaskName(target)) {
        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Generate Native source files for '${target.name}'"
        val compilation = target.compilation
        onlyIf { compilation.compileKotlinTask.enabled }
        this.nativeTarget = compilation.target.konanTarget.name
        title = target.name
        inputClassesDirs = compilation.output.allOutputs

        val nativeKlibDependencies = project.files(project.provider {
            project.configurations.getByName(compilation.defaultSourceSet.implementationMetadataConfigurationName).files
        })
        inputDependencies = compilation.compileDependencyFiles + nativeKlibDependencies

        outputResourcesDir = file("$benchmarkBuildDir/resources")
        outputSourcesDir = file("$benchmarkBuildDir/sources")
    }
}

private fun Project.createNativeBenchmarkCompileTask(target: NativeBenchmarkTarget): KotlinNativeCompilation {

    val compilation = target.compilation
    val benchmarkBuildDir = benchmarkBuildDir(target)
    val compilationTarget = compilation.target
    val benchmarkCompilation =
        compilationTarget.compilations.create(BenchmarksPlugin.BENCHMARK_COMPILATION_NAME) as KotlinNativeCompilation

    // In the previous version of this method a compileTask was changed to build an executable instead of klib.
    // Currently it's impossible to change task output kind and an executable is always produced by
    // a link task. So we disable execution the klib compiling task to save time.
//    benchmarkCompilation.compileKotlinTask.enabled = false

    benchmarkCompilation.compileKotlinTask.dependsOn(generateSourceTaskName(target))

    benchmarkCompilation.apply {
        val sourceSet = kotlinSourceSets.single()
        sourceSet.resources.setSrcDirs(files())
        // TODO: check if there are other ways to set compiler options.
        this.kotlinOptions.freeCompilerArgs = compilation.kotlinOptions.freeCompilerArgs
        sourceSet.kotlin.setSrcDirs(files("$benchmarkBuildDir/sources"))
        sourceSet.dependencies {
            implementation(compilation.compileDependencyFiles)
            implementation(compilation.output.allOutputs)
        }
    }

    compilationTarget.apply {
        binaries {
            // The release build type is already optimized and non-debuggable.
            executable(benchmarkCompilation.name, listOf(RELEASE)) {
                this.compilation = benchmarkCompilation
                this.outputDirectory = file("$benchmarkBuildDir/classes")
                // A link task's name is linkReleaseExecutable<Target>.
                linkTask.apply {
                    group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
                    description = "Compile Native benchmark source files for '${compilationTarget.name}'"
                    dependsOn(generateSourceTaskName(target))
                }
                linkTask.onlyIf { compilation.compileKotlinTask.enabled }
                tasks.getByName(BenchmarksPlugin.ASSEMBLE_BENCHMARKS_TASKNAME).dependsOn(linkTask)
                entryPoint("kotlinx.benchmark.generated.main")
            }
        }
    }
    return benchmarkCompilation
}

fun Project.createNativeBenchmarkExecTask(
    config: BenchmarkConfiguration,
    target: NativeBenchmarkTarget,
    benchmarkCompilation: KotlinNativeCompilation
) {
    task<NativeBenchmarkExec>(
        "${target.name}${config.capitalizedName()}${BenchmarksPlugin.BENCHMARK_EXEC_SUFFIX}",
        depends = config.prefixName(BenchmarksPlugin.RUN_BENCHMARKS_TASKNAME)
    ) {
        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Executes benchmark for '${target.name}'"
        extensions.extraProperties.set("idea.internal.test", System.getProperty("idea.active"))

        val binary =
            benchmarkCompilation.target.binaries.getExecutable(benchmarkCompilation.name, NativeBuildType.RELEASE)
        val linkTask = binary.linkTask
        onlyIf { linkTask.enabled }

        val reportsDir = benchmarkReportsDir(config, target)
        val reportFile = reportsDir.resolve("${target.name}.json")

        val executableFile = linkTask.outputFile.get()
        executable = executableFile.absolutePath
        if (target.workingDir != null)
            workingDir = File(target.workingDir)

        onlyIf { executableFile.exists() }

        dependsOn(linkTask)
        doFirst {
            val ideaActive = (extensions.extraProperties.get("idea.internal.test") as? String)?.toBoolean() ?: false
            args(writeParameters(target.name, reportFile, if (ideaActive) "xml" else "text", config))
            reportsDir.mkdirs()
            logger.lifecycle("Running '${config.name}' benchmarks for '${target.name}'")
        }
    }
}

open class NativeBenchmarkExec : Exec() {
/*
    @Option(option = "filter", description = "Configures the filter for benchmarks to run.")
    var filter: String? = null
*/
}

private fun Project.configureMultiplatformNativeCompilation(
    target: NativeBenchmarkTarget,
    compilation: KotlinNativeCompilation
) {
    val konanTarget = compilation.target.konanTarget

    // Add runtime library as an implementation dependency to the specified compilation
    val runtime =
        dependencies.create("${BenchmarksPlugin.RUNTIME_DEPENDENCY_BASE}-${konanTarget.presetName}:${target.extension.version}")

    compilation.dependencies {
        //implementation(runtime)
    }
}

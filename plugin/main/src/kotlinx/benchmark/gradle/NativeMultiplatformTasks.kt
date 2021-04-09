package kotlinx.benchmark.gradle

import org.gradle.api.*
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.konan.target.*
import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempFile

fun Project.processNativeCompilation(target: NativeBenchmarkTarget) {
    val compilation = target.compilation
    if (compilation.target.konanTarget != HostManager.host) {
        project.logger.warn("Skipping benchmarks for '${target.name}' because they cannot be run on current OS")
        return        
    }
    
    project.logger.info("Configuring benchmarks for '${target.name}' using Kotlin/Native")

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
        extensions.extraProperties.set("idea.internal.test", project.getSystemProperty("idea.active"))

        val binary =
            benchmarkCompilation.target.binaries.getExecutable(benchmarkCompilation.name, NativeBuildType.RELEASE)
        val linkTask = binary.linkTask
        onlyIf { linkTask.enabled }

        val reportsDir = benchmarkReportsDir(config, target)
        reportFile = reportsDir.resolve("${target.name}.json")

        val executableFile = linkTask.outputFile.get()
        executable = executableFile.absolutePath
        this.config = config
        this.workingDir = target.workingDir?.let { File(it) }

        onlyIf { executableFile.exists() }
        benchsDescriptionDir = file(project.buildDir.resolve(target.extension.benchsDescriptionDir).resolve(config.name))
        benchsDescriptionDir.mkdirs()

        val ideaActive = (extensions.extraProperties.get("idea.internal.test") as? String)?.toBoolean() ?: false
        configFile = writeParameters(target.name, reportFile, if (ideaActive) "xml" else "text", config)

        dependsOn(linkTask)
        doFirst {
            reportsDir.mkdirs()
            logger.lifecycle("Running '${config.name}' benchmarks for '${target.name}'")
        }
    }
}

open class NativeBenchmarkExec() : DefaultTask() {
/*
    @Option(option = "filter", description = "Configures the filter for benchmarks to run.")
    var filter: String? = null
*/
    @Input
    lateinit var executable: String

    var workingDir: File? = null

    @Input
    lateinit var configFile: File

    @Input
    lateinit var config: BenchmarkConfiguration

    @Input
    lateinit var reportFile: File

    @Input
    lateinit var benchsDescriptionDir: File

    private fun execute(args: Collection<String>) {
        project.exec {
            it.executable = executable
            it.args(args)
            if (workingDir != null)
                it.workingDir = workingDir
        }
    }

    @ExperimentalPathApi
    @TaskAction
    fun run() {
        // Get full list of running benchmarks
        execute(listOf(configFile.absolutePath, "--list", benchsDescriptionDir.absolutePath))
        val detailedConfigFiles = project.fileTree(benchsDescriptionDir).files.sortedBy { it.absolutePath }
        val jsonReportParts = mutableListOf<File>()
        val runResults = mutableMapOf<String, String>()

        detailedConfigFiles.forEach { runConfig ->
            val runConfigPath = runConfig.absolutePath
            val lines = runConfig.readLines()
            require(lines.size > 1) { "Wrong detailed configuration format" }
            val currentConfigDescription = lines[1]

            // Execute benchmark
            if (config.nativeIterationMode == "internal") {
                val suiteResultsFile = createTempFile("bench", ".txt").toFile()
                execute(listOf(configFile.absolutePath, "--internal", runConfigPath, suiteResultsFile.absolutePath))
                val suiteResults = suiteResultsFile.readText()
                if (suiteResults.isNotEmpty())
                    runResults[runConfigPath] = suiteResults
            } else {
                val iterations = currentConfigDescription.substringAfter("iterations=")
                    .substringBefore(',').toInt()
                val warmups = currentConfigDescription.substringAfter("warmups=")
                    .substringBefore(',').toInt()
                // Warm up
                var exceptionDuringExecution = false
                var textResult: File? = null
                for (i in 0 until warmups) {
                    textResult = createTempFile("bench", ".txt").toFile()
                    execute(listOf(configFile.absolutePath, "--warmup", runConfigPath, i.toString(), textResult.absolutePath))
                    val result = textResult.readLines().getOrNull(0)
                    if (result == "null") {
                        exceptionDuringExecution = true
                        break
                    }
                }
                // Get cycles number
                val cycles = if (!exceptionDuringExecution && textResult != null) textResult.readText() else "1"
                // Execution
                val iterationResults = mutableListOf<Double>()
                var iteration = 0
                while (!exceptionDuringExecution && iteration in 0 until iterations) {
                    val textResult = createTempFile("bench", ".txt").toFile()
                    execute(
                        listOf(configFile.absolutePath, "--iteration", runConfigPath, iteration.toString(),
                            cycles, textResult.absolutePath)
                    )
                    val result = textResult.readLines()[0]
                    if (result == "null")
                        exceptionDuringExecution = true
                    iterationResults.add(result.toDouble())
                    iteration++
                }
                // Store results
                if (iterationResults.size == iterations) {
                    val iterationsResultsFile = createTempFile("bench_results").toFile()
                    iterationsResultsFile.printWriter().use { out ->
                        out.write(iterationResults.joinToString { it.toString() })
                    }
                    execute(
                        listOf(configFile.absolutePath, "--end-run", runConfigPath, iterationsResultsFile.absolutePath)
                    )
                    runResults[runConfigPath] = iterationResults.joinToString()
                }
            }
        }
        // Merge results
        val samplesFile = createTempFile("bench_results").toFile()
        samplesFile.printWriter().use { out ->
            out.write(runResults.toList().joinToString("\n") { "${it.first}: ${it.second}"})
        }
        execute(listOf(configFile.absolutePath, "--store-results", samplesFile.absolutePath))
    }
}

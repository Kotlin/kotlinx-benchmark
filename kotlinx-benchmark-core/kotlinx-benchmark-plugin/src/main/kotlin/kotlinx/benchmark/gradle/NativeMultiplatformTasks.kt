package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import org.gradle.api.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.konan.target.*
import java.io.File
import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.*

@KotlinxBenchmarkPluginInternalApi
fun Project.processNativeCompilation(target: NativeBenchmarkTarget) {
    val compilation = target.compilation
    if (compilation.target.konanTarget != HostManager.host) {
        project.logger.warn("Skipping benchmarks for '${target.name}' because they cannot be run on current OS: Expected ${HostManager.host}, but was ${compilation.target.konanTarget}")
        return
    }

    project.logger.info("Configuring benchmarks for '${target.name}' using Kotlin/Native")

    createNativeBenchmarkGenerateSourceTask(target)

    val benchmarkCompilation = createNativeBenchmarkCompileTask(target)
    target.extension.configurations.forEach {
        createNativeBenchmarkExecTask(it, target, benchmarkCompilation)
    }
}

private fun generateSourceTaskName(target: NativeBenchmarkTarget) =
    target.name + BenchmarksPlugin.BENCHMARK_GENERATE_SUFFIX

private fun Project.createNativeBenchmarkGenerateSourceTask(target: NativeBenchmarkTarget) {
    val benchmarkBuildDir = benchmarkBuildDir(target)
    task<NativeSourceGeneratorTask>(generateSourceTaskName(target)) {
        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Generate Native source files for '${target.name}'"
        val compilation = target.compilation
        this.nativeTarget = compilation.target.konanTarget.name
        title = target.name
        inputClassesDirs = compilation.output.allOutputs

        val nativeKlibDependencies = project.configurations.getByName(compilation.defaultSourceSet.implementationMetadataConfigurationName)
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
        compilationTarget.compilations.create(target.name + BenchmarksPlugin.BENCHMARK_COMPILATION_SUFFIX) as KotlinNativeCompilation

    // In the previous version of this method a compileTask was changed to build an executable instead of klib.
    // Currently it's impossible to change task output kind and an executable is always produced by
    // a link task. So we disable execution the klib compiling task to save time.
//    benchmarkCompilation.compileKotlinTask.enabled = false

    benchmarkCompilation.compileTaskProvider.configure { it.dependsOn(generateSourceTaskName(target)) }

    benchmarkCompilation.apply {
        val sourceSet = kotlinSourceSets.single()

        sourceSet.resources.setSrcDirs(files())
        sourceSet.kotlin.setSrcDirs(files("$benchmarkBuildDir/sources"))

        sourceSet.dependencies {
            implementation(compilation.output.allOutputs)
        }
        project.configurations.let {
            it.getByName(sourceSet.implementationConfigurationName).extendsFrom(
                it.getByName(compilation.compileDependencyConfigurationName)
            )
        }

        // TODO: check if there are other ways to set compiler options.
        this.kotlinOptions.freeCompilerArgs = compilation.kotlinOptions.freeCompilerArgs
    }

    compilationTarget.apply {
        binaries {
            // The release build type is already optimized and non-debuggable.
            executable(benchmarkCompilation.name, listOf(target.buildType)) {
                this.compilation = benchmarkCompilation
                this.outputDirectory = file("$benchmarkBuildDir/classes")
                // A link task's name is linkReleaseExecutable<Target>.
                linkTask.apply {
                    group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
                    description = "Compile Native benchmark source files for '${compilationTarget.name}'"
                    dependsOn(generateSourceTaskName(target))
                }
                tasks.getByName(BenchmarksPlugin.ASSEMBLE_BENCHMARKS_TASKNAME).dependsOn(linkTask)
                entryPoint("kotlinx.benchmark.generated.main")
            }
        }
    }
    return benchmarkCompilation
}

@OptIn(ExperimentalPathApi::class)
@KotlinxBenchmarkPluginInternalApi
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

        val binary =
            benchmarkCompilation.target.binaries.getExecutable(benchmarkCompilation.name, target.buildType)
        val linkTask = binary.linkTask

        dependsOn(linkTask)

        val executableFile = linkTask.outputFile.get()
        onlyIf { executableFile.exists() }

        this.executable = executableFile
        this.nativeFork = config.advanced["nativeFork"] as? String
        this.workingDir = target.workingDir
        this.benchProgressPath = createTempFile("bench", ".txt").absolutePath

        benchsDescriptionDir = project.layout.buildDirectory
            .dir("${target.extension.benchsDescriptionDir}/${config.name}")
            .get().asFile

        reportFile = setupReporting(target, config)
        configFile = writeParameters(target.name, reportFile, traceFormat(), config)

        doFirst {
            benchsDescriptionDir.deleteRecursively()
            benchsDescriptionDir.mkdirs()
        }
    }
}

abstract class NativeBenchmarkExec
@KotlinxBenchmarkPluginInternalApi
@Inject
constructor(
    private val execOperations: ExecOperations,
    private val objectFactory: ObjectFactory,
) : DefaultTask() {
/*
    @Option(option = "filter", description = "Configures the filter for benchmarks to run.")
    var filter: String? = null
*/
    @InputFile
    lateinit var executable: File

    @Input
    @Optional
    var workingDir: String? = null

    @InputFile
    lateinit var configFile: File

    @Input
    @Optional
    var nativeFork: String? = null

    @OutputFile
    lateinit var reportFile: File

    @Internal
    lateinit var benchsDescriptionDir: File

    @Internal
    lateinit var benchProgressPath: String

    private fun execute(args: Collection<String>) {
        execOperations.exec {
            it.executable = executable.absolutePath
            it.args(args)
            workingDir?.let { dir ->
                it.workingDir = File(dir)
            }
        }
    }

    @OptIn(ExperimentalPathApi::class)
    @TaskAction
    fun run() {
        // Get full list of running benchmarks
        execute(listOf(configFile.absolutePath, "--list", benchProgressPath, benchsDescriptionDir.absolutePath))
        val detailedConfigFiles = objectFactory.fileTree().from(benchsDescriptionDir).files.sortedBy { it.absolutePath }
        val runResults = mutableMapOf<String, String>()

        val forkPerBenchmark = nativeFork.let { it == null || it == "perBenchmark" }

        detailedConfigFiles.forEach { runConfig ->
            val runConfigPath = runConfig.absolutePath
            val lines = runConfig.readLines()
            require(lines.size > 1) { "Wrong detailed configuration format" }
            val currentConfigDescription = lines[1]

            // Execute benchmark
            if (forkPerBenchmark) {
                val suiteResultsFile = createTempFile("bench", ".txt")
                execute(listOf(configFile.absolutePath, "--benchmark", benchProgressPath, runConfigPath, suiteResultsFile.absolutePath))
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
                var textResult: Path? = null
                for (i in 0 until warmups) {
                    textResult = createTempFile("bench", ".txt")
                    execute(listOf(configFile.absolutePath, "--warmup", benchProgressPath, runConfigPath, i.toString(), textResult.absolutePath))
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
                    textResult = createTempFile("bench", ".txt")
                    execute(
                        listOf(configFile.absolutePath, "--iteration", benchProgressPath, runConfigPath, iteration.toString(),
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
                    val iterationsResultsFile = createTempFile("bench_results")
                    iterationsResultsFile.bufferedWriter().use { out ->
                        out.write(iterationResults.joinToString { it.toString() })
                    }
                    execute(
                        listOf(configFile.absolutePath, "--end-run", benchProgressPath, runConfigPath, iterationsResultsFile.absolutePath)
                    )
                    runResults[runConfigPath] = iterationResults.joinToString()
                }
            }
        }
        // Merge results
        val samplesFile = createTempFile("bench_results")
        samplesFile.bufferedWriter().use { out ->
            out.write(runResults.toList().joinToString("\n") { "${it.first}: ${it.second}"})
        }
        execute(listOf(configFile.absolutePath, "--store-results", benchProgressPath, samplesFile.absolutePath))
    }
}

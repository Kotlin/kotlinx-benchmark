package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.internal.android.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.provider.*
import org.gradle.api.tasks.*
import java.io.*
import java.util.*
import kotlin.text.get
import kotlin.time.Duration.Companion.milliseconds

/**
 * Task that runs benchmarks on a connected Android device via a spawned Gradle process,
 * then pulls and consolidates the results.
 *
 * Each benchmark descriptor class is run in its own `connectedReleaseAndroidTest` invocation
 * to avoid exceeding gRPC limits when retriving test results from the device. Results are
 * accumulated and written as a single consolidated summary at the end.
 */
internal abstract class AndroidExecTask : DefaultTask() {

    init {
        // Regardless of any caches, we always want to run when `androidBenchmark` is triggered.
        outputs.upToDateWhen { false }
    }

    // Absolute path to the `adb` executable.
    @get:Input
    abstract val adb: Property<String>

    @get:Input
    abstract val timeoutMs: Property<Long>

    // Root directory of the generated Microbenchmark Gradle project to execute.
    @get:InputDirectory
    abstract val benchmarkProjectDir: DirectoryProperty

    // If `true`, no output will be generated.
    @get:Input
    abstract val dryRun: Property<Boolean>

    /**
     * Directory on the host where the Jetpack Microbenchmark plugin copies device results.
     * Not declared as a task input because it is populated by the spawned Gradle process
     * during this task's execution.
     */
    @get:Internal
    abstract val deviceOutputDir: DirectoryProperty

    /** Directory where the consolidated benchmark reports are written. */
    @get:OutputDirectory
    abstract val benchmarkResultsDir: DirectoryProperty

    // Jetpack Microbenchmark has their own format when outputting results, but we can use the
    // knowledge about our own naming schema for the generated code to map back to something that
    // more user-friendly. I.e., we know that anything after this separator is the user's benchmark
    // method name.
    // Changes to `AndroidBenchmarkGenerator.generateDescriptorFile()` should be reflected here.
    private val benchmarkResultSeparator = "_Descriptor.benchmark_"

    @TaskAction
    fun exec() {
        val adbPath = adb.get()
        detectAndroidDevice(adbPath)

        val benchmarkClasses = discoverBenchmarkClasses()
        val projectDir = benchmarkProjectDir.get().asFile
        val resultsDir = benchmarkResultsDir.get().asFile
        resultsDir.deleteRecursively()
        resultsDir.mkdirs()

        if (benchmarkClasses.isEmpty()) {
            logger.warn("No benchmark classes discovered; nothing to run.")
            return
        }

        // Track accumulated results across all benchmark classes
        val allWarnings = mutableMapOf<String, LinkedHashSet<String>>()
        val allResults = mutableMapOf<String, MutableList<AndroidBenchmarkResult>>()

        for (fqcn in benchmarkClasses) {
            val args = listOf(
                "connectedReleaseAndroidTest",
                "-Pandroid.testInstrumentationRunnerArguments.class=$fqcn",
                "--stacktrace"
            )
            val userFqcn = fqcn.removeSuffix("_Descriptor")
            logger.lifecycle("Running $userFqcn")
            val (completed, exitCode) = runBenchmarkProjectGradleTask(
                projectDir = projectDir,
                args = args,
                showTaskOutputAsLifecycle = false,
                timeoutMs = timeoutMs.get()
            )
            when {
                !completed -> throw GradleException("Android benchmark failed to complete in the allocated time: ${timeoutMs.get().milliseconds}")
                exitCode != 0 -> throw GradleException("Android benchmark failed with exit code: $exitCode")
            }

            val deviceDir = deviceOutputDir.get().asFile
            if (deviceDir.exists()) {
                collectBenchmarkClassResults(deviceDir, userFqcn, resultsDir, allWarnings, allResults)
                deviceDir.deleteRecursively()
            } else if (!dryRun.get()) {
                throw GradleException("Directory containing Android device benchmark results was not found: ${deviceDir.absolutePath}")
            }
        }

        when {
            dryRun.get() -> logger.lifecycle("Android benchmarks finished using `dryRun`. No results were created.")
            allResults.isEmpty() -> throw GradleException("No benchmark results found")
            else -> {
                writeConsolidatedSummary(resultsDir, allWarnings, allResults)
                logger.lifecycle("Android benchmarks finished. Results were saved in: ${resultsDir.absolutePath}")
            }
        }
    }

    /**
     * Discovers fully qualified names of all generated benchmark descriptor classes by scanning
     * the generated Kotlin source files in the microbenchmark project.
     *
     * The simple approach should be safe as the Annotation Processor guarantees that only benchmark
     * classes will be written here.
     */
    private fun discoverBenchmarkClasses(): List<String> {
        val sourceDir = benchmarkProjectDir.get().asFile
            .resolve("microbenchmark/src/androidTest/kotlin")
        if (!sourceDir.exists()) return emptyList()
        return sourceDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .map { file ->
                val packageName = file.useLines { line ->
                    line.firstOrNull()?.removePrefix("package ")?.trim()
                }
                if (packageName != null) "$packageName.${file.nameWithoutExtension}" else file.nameWithoutExtension
            }
            .toList()
    }

    private fun detectAndroidDevice(adb: String) {
        logger.info("Detect running Android devices...")
        val devices = ProcessBuilder(adb, "devices")
            .start()
            .inputStream
            .bufferedReader()
            .useLines { lines ->
                lines.filter { it.endsWith("device") }
                    .map { it.substringBefore("\t") }
                    .toList()
            }
        devices.takeIf { it.isNotEmpty() }
            ?.let {
                logger.info("Connected Android devices/emulators:\n\t${it.joinToString("\n\t")}")
            } ?: throw RuntimeException("No Android devices/emulators found, please start an emulator or connect a device.")
    }

    /**
     * Called after each per-class benchmark run. Parses the device output directory to accumulate
     * warnings and benchmark result lines, and copies all raw Jetpack Microbenchmark output (json,
     * traces, results) into `resultsDir/<deviceId>/<classFQN>/` for later reference by users.
     */
    private fun collectBenchmarkClassResults(
        deviceOutputDir: File,
        qualifiedUserClassName: String,
        resultsDir: File,
        allWarnings: MutableMap<String, LinkedHashSet<String>>,
        allResults: MutableMap<String, MutableList<AndroidBenchmarkResult>>
    ) {
        deviceOutputDir.listFiles()
            ?.filter { it.isDirectory }
            ?.forEach { deviceDir ->
                val deviceId = deviceDir.name
                val warnings = allWarnings.getOrPut(deviceId) { linkedSetOf() }
                val resultLines = allResults.getOrPut(deviceId) { mutableListOf() }

                deviceDir.walkTopDown()
                    .filter { it.isFile && it.extension == "txt" }
                    .sorted()
                    .forEach { txtFile ->
                        val currentWarning = mutableListOf<String>()
                        txtFile.forEachLine { line ->
                            when {
                                // A new WARNING is started, or we are still collecting
                                // information about a warning
                                (line.startsWith("WARNING") || currentWarning.isNotEmpty()) && line.isNotBlank() -> {
                                    currentWarning += line
                                }

                                // Empty line ends the current warning block
                                currentWarning.isNotEmpty() && line.isBlank() -> {
                                    warnings += currentWarning.joinToString(System.lineSeparator())
                                    currentWarning.clear()
                                }

                                // Fuzzy matching of benchmark results.
                                line.contains(benchmarkResultSeparator) -> {
                                    val measurement = parseBenchmarkLine(qualifiedUserClassName, line)
                                    if (measurement != null) resultLines += measurement
                                }
                            }
                        }
                        // Handle warning block at end of file with no trailing empty line
                        if (currentWarning.isNotEmpty()) {
                            warnings += currentWarning.joinToString(System.lineSeparator())
                        }
                    }

                // Copy all raw Jetpack output (json, traces, etc.) into <deviceId>/<ClassName>/
                val classOutputDir = resultsDir.resolve("$deviceId/$qualifiedUserClassName")
                classOutputDir.mkdirs()
                deviceDir.copyRecursively(classOutputDir, overwrite = true)

                // Also copy the JSON to the top level as <deviceId>-<com.example.UserClass>.json
                deviceDir.walkTopDown()
                    .firstOrNull { it.isFile && it.extension == "json" }
                    ?.copyTo(resultsDir.resolve("$deviceId-$qualifiedUserClassName.json"), overwrite = true)
            }
    }

    /**
     * Writes one `<deviceId>.txt` summary file per device, containing all accumulated
     * benchmark results sorted by name, with any warnings prepended.
     */
    private fun writeConsolidatedSummary(
        resultsDir: File,
        allWarnings: Map<String, Set<String>>,
        allResults: Map<String, List<AndroidBenchmarkResult>>
    ) {
        allResults.keys.forEach { deviceId ->
            val summaryFile = resultsDir.resolve("$deviceId.txt")
            summaryFile.bufferedWriter().use { writer ->
                allWarnings[deviceId]?.forEach { block ->
                    writer.append(block)
                    writer.newLine()
                }
                if (allWarnings[deviceId]?.isNotEmpty() == true) writer.newLine()
                allResults[deviceId]?.sortedBy { it.benchmarkName }?.forEach {
                    writer.appendLine(it.toString())
                }
            }
        }
    }

    /**
     * Parses a Jetpack Benchmark result line such as:
     *
     * `0.3 ns           0 allocs    [Trace](file://...)    [Method Trace](file://...)    EMULATOR_MyBenchmark_Descriptor.benchmark_myMethod[params]`
     *
     * Returns a simplified line:
     *
     * `0.3 ns    0 allocs    com.userpackage.MyBenchmark.myMethod[params]`
     *
     * Or null if the line cannot be parsed.
     *
     * Jetpack uses a fixed column format for each measurement type, but there are inconsistencies
     * between test files and some miss the first whitespace. It does look like we can depend on
     * fixed 4 whitespace between each metadata section.
     */
    private fun parseBenchmarkLine(className: String, line: String): AndroidBenchmarkResult? {
        val match = benchmarkLineRegex.matchEntire(line.trim())
        if (match == null) {
            logger.debug("Skipping unidentified line: $line")
            return null
        }
        val runtime = match.groups["time"]!!.value
        val allocs = match.groups["alloc"]?.value
        val benchmarkName = match.groups["metadata"]!!.value.let { metadata ->
            val metadataParts = metadata.split("\\s{4}".toPattern())
            val benchmarkIdParts = metadataParts.last()
            val methodName = benchmarkIdParts.substringAfter(benchmarkResultSeparator)
            "$className.$methodName"
        }
        return AndroidBenchmarkResult(runtime, allocs, benchmarkName)
    }
}

private val benchmarkLineRegex = Regex(
    "(?<time>[\\d.,]+\\s+[\\w.]+)\\s*(?<alloc>[\\d.,]+\\s+[\\w.]+)?\\s*(?<metadata>.*)"
)

private data class AndroidBenchmarkResult(
    val runtime: String,
    val allocs: String?,
    val benchmarkName: String
) {
    override fun toString(): String = buildString {
        append(runtime)
        if (allocs != null) {
            append(SEPARATOR)
            append(allocs)
        }
        append(SEPARATOR)
        append(benchmarkName)
    }

    companion object {
        private const val SEPARATOR = "    "
    }
}

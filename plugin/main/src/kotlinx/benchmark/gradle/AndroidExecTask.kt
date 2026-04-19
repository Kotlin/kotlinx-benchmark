package kotlinx.benchmark.gradle

import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.provider.*
import org.gradle.api.tasks.*
import java.io.*
import java.util.*
import java.util.concurrent.*
import kotlin.text.get

/**
 * Task that runs benchmarks on a connected Android device via a spawned Gradle process,
 * then pulls and consolidates the results.
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

    // Human-readable timeout string used in error messages.
    @get:Input
    abstract val timeoutStr: Property<String>

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

    @TaskAction
    fun exec() {
        val adbPath = adb.get()
        detectAndroidDevice(adbPath)

        val executeBenchmarkPath = benchmarkProjectDir.get().asFile.path
        // Using ./gradlew on Windows shows error:
        // CreateProcess error=193, %1 is not a valid Win32 application
        val osName = System.getProperty("os.name").lowercase(Locale.ROOT)
        val gradlewPath = "$executeBenchmarkPath/gradlew" + if (osName.contains("win")) ".bat" else ""
        val args = listOf("-p", executeBenchmarkPath, "connectedReleaseAndroidTest", "--stacktrace")

        logger.info("Running command: $gradlewPath ${args.joinToString(" ")}")

        val process = ProcessBuilder(gradlewPath, *args.toTypedArray())
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        val outputGobbler = StreamGobbler(process.inputStream) {
            // Almost all log output from the spawned Gradle process will be sent to the
            // normal input stream, regardless of log level. As we don't have a great way
            // to filter it on this side, we just send all of it to the debug log, as it
            // can be helpful during debugging, but otherwise just pollute the main build log.
            logger.info(it)
        }
        val errorGobbler = StreamGobbler(process.errorStream) {
            logger.error(it)
        }

        outputGobbler.start()
        errorGobbler.start()

        clearLogcat(adbPath)
        val success = process.waitFor(timeoutMs.get(), TimeUnit.MILLISECONDS)
        captureLogcatOutput(adbPath)
        val androidResultDir = when (success && process.exitValue() == 0) {
            true -> {
                logger.debug("Looking for logs in: ${deviceOutputDir.asFile.get().absolutePath}")
                processDeviceBenchmarkResults(deviceOutputDir.get().asFile, benchmarkResultsDir.get().asFile)
            }
            false -> null
        }
        when {
            !success -> throw GradleException("Android benchmark task failed complete in the allocated time: ${timeoutStr.get()}")
            process.exitValue() != 0 -> throw GradleException("Android benchmark task failed with exit code: ${process.exitValue()}")
            (androidResultDir == null) -> {
                when (!dryRun.get()) {
                    true -> throw GradleException("Directory containing Android device benchmark results was not found: ${deviceOutputDir.asFile.get().absolutePath}")
                    false -> logger.lifecycle("Android benchmarks finished using `dryRun`. No results were created.")
                }
            }
            else -> logger.lifecycle("Android benchmarks finished. Results were saved in: ${androidResultDir.absolutePath}")
        }
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
     * Copy and process device benchmark files. The path to the final reports dir is returned, or `null` if an error
     * occurred.
     *
     * To avoid outputs from different runs being mixed, we always delete the entire report directory before copying the
     * new device files. This way we don't risk seeing old data mixed with new.
     *
     * Jetpack Benchmark produces quite a lot of output files. We copy all of them per device, allowing users to use their
     * own tooling on a format they might know already.
     *
     * On top of this, we create a consolidated `<deviceId>.txt` file that contains a summary of the benchmark results, in
     * a similar format to how Jetpack reports them.
     *
     * Jetpack also generates a `.json` file containing all the underlying benchmark data. Unfortunately, it is not in a
     * JMH-compatible format, which would be needed to support the `reportFormat` configuration setting.
     *
     * For now, we ignore the `reportFormat` configuration and just copy the JSON file directly.
     */
    private fun processDeviceBenchmarkResults(deviceOutputDir: File, benchmarkResultsDir: File): File? {
        if (!deviceOutputDir.exists()) {
            return null
        }

        benchmarkResultsDir.deleteRecursively()
        benchmarkResultsDir.mkdirs()
        deviceOutputDir.copyRecursively(benchmarkResultsDir)

        // For each device id, produce a consolidated summary file and make the underlying Jetpack data more visible
        deviceOutputDir.listFiles()
            ?.filter { it.isDirectory }
            ?.forEach { deviceDir ->
                val deviceId = deviceDir.name
                val warnings = linkedSetOf<String>()
                val resultLines = mutableListOf<AndroidBenchmarkResult>()

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
                                // Changes to `AndroidBenchmarkGenerator.generateDescriptorFile()` should be reflected here.
                                line.contains("_Descriptor.benchmark_") -> {
                                    val simplified = parseBenchmarkLine(line)
                                    if (simplified != null) resultLines += simplified
                                }
                            }
                        }
                        // Handle warning block at end of file with no trailing empty line
                        if (currentWarning.isNotEmpty()) {
                            warnings += currentWarning.joinToString(System.lineSeparator())
                        }
                    }

                // Create a summary file at same level as the device-id directory
                val summaryFile = benchmarkResultsDir.resolve("$deviceId.txt")
                summaryFile.bufferedWriter().use { writer ->
                    if (warnings.isNotEmpty()) {
                        warnings.forEach { block ->
                            writer.append(block)
                            writer.newLine()
                        }
                        writer.newLine()
                    }
                    resultLines.sortedBy { it.benchmarkName }.forEach {
                        writer.appendLine(it.toString())
                    }
                }

                // Move the Jetpack Benchmark data file to the same level as the summary file
                deviceOutputDir
                    .walkTopDown()
                    .firstOrNull { it.isFile && it.extension == "json" }
                    ?.let { file ->
                        val destination = benchmarkResultsDir.resolve("$deviceId.json")
                        file.copyTo(destination, overwrite = true)
                    }
            }

        return benchmarkResultsDir
    }

    /**
     * Parses a Jetpack Benchmark result line such as:
     *
     * `0.3 ns           0 allocs    [Trace](file://...)    [Method Trace](file://...)    EMULATOR_MyBenchmark_Descriptor.benchmark_MyBenchmark_myMethod[params]`
     *
     * Returns a simplified line:
     *
     * `0.3 ns    0 allocs    MyBenchmark.myMethod[params]`
     *
     * Or null if the line cannot be parsed.
     *
     * Jetpack uses a fixed column format for each measurement type, but there are inconsistencies
     * between test files and some miss the first whitespace. It does look like we can depend on
     * fixed 4 whitespace between each metadata section.
     */
    private fun parseBenchmarkLine(line: String): AndroidBenchmarkResult? {
        val match = benchmarkLineRegex.matchEntire(line.trim())
        if (match == null) {
            logger.debug("Skipping unidentified line: $line")
            return null
        }
        val runtime = match.groups["time"]!!.value
        val allocs = match.groups["alloc"]?.value
        val benchmarkName = match.groups["metadata"]!!.value.let { metadata ->
            val metadataParts = metadata.split("\\s{4}".toPattern())
            val benchmarkIdParts = metadataParts.last().split("_")
            val className = benchmarkIdParts[benchmarkIdParts.lastIndex - 1]
            val methodName = benchmarkIdParts.last()
            "$className.$methodName"
        }
        return AndroidBenchmarkResult(runtime, allocs, benchmarkName)
    }

    private fun captureLogcatOutput(adb: String) {
        try {
            val logcatProcess = ProcessBuilder(adb, "logcat", "TestRunner:D", "KotlinBenchmark:D", "*:S")
                .redirectErrorStream(true)
                .start()

            val logcatGobbler = StreamGobbler(logcatProcess.inputStream) { line ->
                when {
                    line.contains("started") ->
                        logger.debug(
                            "Android: " +
                                    line.substringAfter("started: ")
                                        .substringBefore("(")
                                        .replace(Regex("\\[\\d+: "), "[")
                        )
                }
            }

            logcatGobbler.start()
            if (!logcatProcess.waitFor(10, TimeUnit.SECONDS)) {
                logcatProcess.destroy()
            }
            logcatGobbler.join()
        } catch (e: Exception) {
            throw GradleException("Failed to capture logcat output", e)
        }
    }

    private fun clearLogcat(adb: String) {
        try {
            val success = ProcessBuilder(adb, "logcat", "-c")
                .redirectErrorStream(true)
                .start()
                .waitFor(5, TimeUnit.SECONDS)
            if (!success) {
                throw GradleException("Failed to clear logcat within the allocated time: 5 seconds")
            }
        } catch (e: Exception) {
            throw GradleException("Failed to clear logcat", e)
        }
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

private class StreamGobbler(private val inputStream: InputStream, private val consumer: (String) -> Unit) : Thread() {
    override fun run() {
        inputStream.bufferedReader().useLines { lines ->
            lines.forEach { consumer(it) }
        }
    }
}

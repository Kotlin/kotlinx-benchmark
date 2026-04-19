package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.internal.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.provider.*
import org.gradle.api.tasks.*
import java.io.*
import javax.inject.*

/**
 * Task that creates an empty skeleton Jetpack Microbenchmark Gradle project used to run
 * `kotlinx-benchmark` tests on Android.
 *
 * Benchmark files will be added when calling [AndroidGeneratorTask].
 */
internal abstract class AndroidSetupBenchmarkProject @Inject constructor(
    private val fs: FileSystemOperations,
    private val archives: ArchiveOperations,
) : DefaultTask() {

    companion object {
        // Name of the template folder inside the plugin JAR
        private const val ANDROID_BENCHMARK_PROJECT_TEMPLATE = "androidProjectTemplate"
    }

    // Template configuration used to set up the skeleton project.
    @get:Input
    abstract val configuration: Property<AndroidBenchmarkTarget.TemplateConfiguration>

    // Reference to the AAR or JAR file produced by the main project's Android compilation.
    @get:InputFile
    abstract val libraryFile: RegularFileProperty

    // Transitive runtime dependencies of the benchmarked library to include in the generated project.
    @get:InputFiles
    abstract val dependencies: ConfigurableFileCollection

    // `kotlinx-benchmark` plugin JAR file which contains the template files.
    @get:InputFile
    abstract val pluginJar: RegularFileProperty

    // Root directory of the generated Gradle project.
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun setup() {
        val dir = outputDir.get().asFile
        logger.info("Setting up an Android benchmark project in $dir")
        fs.sync { spec ->
            with(spec) {
                from(archives.zipTree(pluginJar))
                into(dir)
                include("$ANDROID_BENCHMARK_PROJECT_TEMPLATE/**")
                eachFile { fileCopy ->
                    fileCopy.path = fileCopy.path.removePrefix("$ANDROID_BENCHMARK_PROJECT_TEMPLATE/")
                }
                includeEmptyDirs = false
            }
        }
        dir.resolve("gradlew").setExecutable(true)
        dir.resolve("gradlew.bat").setExecutable(true)
        writeTemplateValues(dir)
    }

    private fun writeTemplateValues(generatedProjectDir: File) {
        val config = configuration.get()
        val libFile = libraryFile.get().asFile
        check(libFile.exists()) { "Library file not found: ${libFile.absolutePath}" }

        updateTemplateValues(generatedProjectDir.resolve("microbenchmark/build.gradle.kts")) {
            // Right now this list exposes the most "relevant" API's. Which ones to
            // expose probably requires more user feedback.
            mapOf(
                "JVM_TARGET" to "JvmTarget.${config.jvmTargetName}",
                "JVM_TOOLCHAIN" to config.jvmToolchain.toString(),
                "NAMESPACE" to config.namespace,
                "ANDROID_COMPILE_SDK" to config.compileSdk.toString(),
                "ANDROID_MIN_SDK" to config.minSdk.toString(),
                "BENCHMARKED_AAR_ABSOLUTE_PATH" to libFile.absolutePath.replace("\\", "/"),
                "TEST_INSTRUMENTATION_RUNNER_ARGUMENTS" to gatherInstrumentationRunnerArguments(config),
                "ADDITIONAL_DEPENDENCIES" to buildAdditionalDependencyLines(dependencies.files),
            )
        }

        updateTemplateValues(generatedProjectDir.resolve("gradle/libs.versions.toml")) {
            mapOf(
                "KOTLIN_VERSION" to config.kotlinVersion,
                "ANDROID_GRADLE_PLUGIN_VERSION" to config.agpVersion,
                "ANDROIDX_BENCHMARK_VERSION" to BenchmarksPluginConstants.DEFAULT_ANDROIDX_BENCHMARK_VERSION,
                "ANDROIDX_TEST_EXT_VERSION" to BenchmarksPluginConstants.DEFAULT_ANDROIDX_TEST_EXT_VERSION,
            )
        }

        updateTemplateValues(generatedProjectDir.resolve("gradle/wrapper/gradle-wrapper.properties")) {
            mapOf(
                "GRADLE_VERSION" to config.gradleVersion,
            )
        }

        generatedProjectDir.resolve("local.properties").let { file ->
            val sdkPath = config.sdkDir
            if (sdkPath.isNullOrBlank()) {
                throw GradleException("Android SDK path is not set. Please set ANDROID_HOME environment variable or specify sdkPath in the build script.")
            } else {
                file.writeText("sdk.dir=${sdkPath.replace("\\", "/")}\n")
                logger.info("SDK path written to local.properties: ${file.readText()}")
            }
        }
    }

    private fun gatherInstrumentationRunnerArguments(config: AndroidBenchmarkTarget.TemplateConfiguration): String {
        val arguments = config.instrumentationRunnerArguments.toMutableMap()

        val mode = config.profilingMode
        if (mode != ProfilingMode.Default) {
            val key = "androidx.benchmark.profiling.mode"
            if (arguments.containsKey(key)) {
                logger.warn("Instrumentation runner argument '$key' is already set through generic blob, ignoring profiling mode setting")
            }
            arguments[key] = when (mode) {
                ProfilingMode.MethodTracing -> "MethodTracing"
                ProfilingMode.StackSampling -> "StackSampling"
                ProfilingMode.None -> "None"
                ProfilingMode.Default -> error("Default profiling mode not supported here")
            }
        }

        if (arguments.containsKey("androidx.benchmark.dryRunMode.enable")) {
            logger.warn("Instrumentation runner argument 'androidx.benchmark.dryRunMode.enable' is ignored. Set it through `AndroidBenchmarkTarget.dryRun`")
        }
        arguments["androidx.benchmark.dryRunMode.enable"] = config.dryRun.toString()

        return arguments.entries
            .joinToString(System.lineSeparator()) { (key, value) ->
                """testInstrumentationRunnerArguments["$key"]="$value""""
            }
    }

    private fun buildAdditionalDependencyLines(files: Set<File>): String =
        files
            .filter { it.exists() }
            .joinToString(System.lineSeparator()) { file ->
                """    androidTestImplementation(files("${file.absolutePath.replace("\\", "/")}"))"""
            }

    private fun updateTemplateValues(templateFile: File, block: () -> Map<String, String>) {
        if (!templateFile.exists()) {
            throw IllegalStateException("Template file does not exist: ${templateFile.absolutePath}")
        }
        val replacements = block()
        val pattern = Regex(replacements.keys.joinToString("|") {
            Regex.escape("<<$it>>")
        })
        val template = templateFile.readText()
        val updatedTemplate = pattern.replace(template) {
            replacements.getValue(it.value.removeSurrounding("<<", ">>"))
        }
        templateFile.writeText(updatedTemplate)
    }
}
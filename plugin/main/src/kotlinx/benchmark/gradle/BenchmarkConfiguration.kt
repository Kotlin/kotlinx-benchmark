package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.internal.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.provider.*
import org.gradle.api.tasks.*
import org.gradle.util.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.js.ir.*
import java.io.*
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*
import kotlin.time.*
import kotlin.time.Duration.Companion.minutes

open class BenchmarkConfiguration
@KotlinxBenchmarkPluginInternalApi
constructor(
    @property:KotlinxBenchmarkPluginInternalApi
    val extension: BenchmarksExtension,
    val name: String,
) {
    var iterations: Int? = null
    var warmups: Int? = null
    var iterationTime: Long? = null
    var iterationTimeUnit: String? = null
    var mode: String? = null
    var outputTimeUnit: String? = null
    var reportFormat: String? = null

    var includes: MutableList<String> = mutableListOf()
    var excludes: MutableList<String> = mutableListOf()
    var params: MutableMap<String, MutableList<Any?>> = mutableMapOf()
    var advanced: MutableMap<String, Any?> = mutableMapOf()

    fun include(pattern: String) {
        includes.add(pattern)
    }

    fun exclude(pattern: String) {
        excludes.add(pattern)
    }

    fun param(name: String, vararg value: Any?) {
        val values = params.getOrPut(name) { mutableListOf() }
        values.addAll(value)
    }

    fun advanced(name: String, value: Any?) {
        advanced[name] = value
    }

    @KotlinxBenchmarkPluginInternalApi
    fun capitalizedName() = if (name == "main") "" else name.replaceFirstChar { it.titlecase() }

    @KotlinxBenchmarkPluginInternalApi
    fun prefixName(suffix: String) = if (name == "main") suffix else name + suffix.replaceFirstChar { it.titlecase() }

    @KotlinxBenchmarkPluginInternalApi
    fun reportFileExt(): String = reportFormat?.lowercase() ?: "json"
}

// Base class for all targets supported by `kotlinx-benchmark`.
// WARNING: This class is _not_ configuration-cache safe.
open class BenchmarkTarget
@KotlinxBenchmarkPluginInternalApi
constructor(
    @property:KotlinxBenchmarkPluginInternalApi
    val extension: BenchmarksExtension,
    val name: String,
) {
    var workingDir: String? = null
}

abstract class JvmBenchmarkTarget
@KotlinxBenchmarkPluginInternalApi
constructor(
    extension: BenchmarksExtension,
    name: String
) : BenchmarkTarget(extension, name) {
    var jmhVersion: String = extension.project.providers.gradleProperty("benchmarks_jmh_version")
        .orElse(BenchmarksPluginConstants.DEFAULT_JMH_VERSION).get()
}

class JavaBenchmarkTarget
@KotlinxBenchmarkPluginInternalApi
constructor(
    extension: BenchmarksExtension,
    name: String,
    @property:KotlinxBenchmarkPluginInternalApi
    val sourceSet: SourceSet
) : JvmBenchmarkTarget(extension, name)

open class KotlinJvmBenchmarkTarget
@KotlinxBenchmarkPluginInternalApi
constructor(
    extension: BenchmarksExtension,
    name: String,
    @property:KotlinxBenchmarkPluginInternalApi
    val compilation: KotlinJvmCompilation
) : JvmBenchmarkTarget(extension, name)

enum class JsBenchmarksExecutor {
    BenchmarkJs,
    BuiltIn
}

class JsBenchmarkTarget
@KotlinxBenchmarkPluginInternalApi
constructor(
    extension: BenchmarksExtension,
    name: String,
    @property:KotlinxBenchmarkPluginInternalApi
    val compilation: KotlinJsIrCompilation
) : BenchmarkTarget(extension, name) {
    var jsBenchmarksExecutor: JsBenchmarksExecutor = JsBenchmarksExecutor.BenchmarkJs
}

class WasmBenchmarkTarget
@KotlinxBenchmarkPluginInternalApi
constructor(
    extension: BenchmarksExtension,
    name: String,
    @property:KotlinxBenchmarkPluginInternalApi
    val compilation: KotlinJsIrCompilation
) : BenchmarkTarget(extension, name)

class NativeBenchmarkTarget
@KotlinxBenchmarkPluginInternalApi
constructor(
    extension: BenchmarksExtension,
    name: String,
    @property:KotlinxBenchmarkPluginInternalApi
    val compilation: KotlinNativeCompilation
) : BenchmarkTarget(extension, name) {
    var buildType: NativeBuildType = NativeBuildType.RELEASE
}

/**
 * Android benchmarks run through an auto-generated Jetpack Microbenchmark Gradle project.
 * This API makes it possible to configure this project.
 *
 * In case of build errors, the project files can be found and reviewed in `build/benchmarks/android/`.
 */
class AndroidBenchmarkTarget
@KotlinxBenchmarkPluginInternalApi
internal constructor(
    extension: BenchmarksExtension,
    name: String,
    private val target: KmpAndroidTargetCompat,
    compilation: KotlinCompilation<*>,
    mainProjectGradleWrapperPropertiesFile: File,
    mainProjectGradleVersion: GradleVersion,
    mainProjectKotlinVersion: String,
    mainProjectAgpVersion: String,
    adbReference: Provider<RegularFile>,
) : BenchmarkTarget(extension, name) {

    /**
     * Sets the [ProfilingMode] for the benchmark.
     */
    public var profilingMode: ProfilingMode = ProfilingMode.Default

    /**
     * If `true`, the benchmark will be run with the `dryRun` flag. This means that each test
     * only runs once and no output is generated.
     *
     * See [https://developer.android.com/topic/performance/benchmarking/microbenchmark-instrumentation-args#dryrunmode-enable]
     */
    public var dryRun: Boolean = false

    /**
     * Configures any extra `instrumentationRunnerArguments` arguments you want to set on the generated
     * microbenchmark project.
     *
     * See the list of valid arguments here:
     * https://developer.android.com/topic/performance/benchmarking/microbenchmark-instrumentation-args
     */
    public val instrumentationRunnerArguments: MutableMap<String, String> = mutableMapOf()

    /**
     * Sets the path to the Android SDK directory.
     * If not set, the plugin will first attempt to read it from the `sdk.dir` gradle property and then
     * from the `ANDROID_HOME` environment variable.
     */
    public val sdkDir: Property<String> = extension.project.objects.property(String::class.java).convention(
        extension.project.providers.gradleProperty("sdk.dir")
            .orElse(extension.project.providers.environmentVariable("ANDROID_HOME"))
            .orElse("")
    )

    /**
     * How long kotlinx-benchmark will wait for the device to complete benchmarks for a single class.
     * If the limit is reached, the benchmark run will be aborted and an exception is thrown.
     */
    public var timeout: Duration = 10.minutes

    /**
     * Absolute path to the `adb` executable used by the benchmark infrastructure
     */
    internal val adb: String = adbReference.get().asFile.absolutePath

    /**
     * Benchmark results will automatically be pulled from the device by the Jetpack Microbenchmark plugin.
     * This folder is semi-stable but might change between versions of Jetpack Microbenchmarks. The destination
     * can configured here. The relative path is from the `build` directory.
     *
     * If `dryRun` is enabled, no output will be generated.
     *
     * See [https://developer.android.com/topic/performance/benchmarking/microbenchmark-write?utm_source=chatgpt.com#benchmark-results]]
     */
    internal val deviceResultOutputDirectory: Path = Path("outputs/connected_android_test_additional_output/releaseAndroidTest/connected")

    /**
     * The JVM Target used by the benchmark library.
     * The default value is the same JVM Target used by the `androidLibrary` target in the main project.
     */
    @Suppress("DEPRECATION")
    internal val jvmTarget: JvmTarget = (compilation.compilerOptions.options as KotlinJvmCompilerOptions).jvmTarget.get()

    /**
     * Version of Kotlin used by the generated benchmark project. S
     * The default value is the same Kotlin version used by the main project.
     */
    internal val kotlinVersion: String = mainProjectKotlinVersion

    /**
     * Version of the Android Gradle Plugin used by the generated benchmark project.
     * The default value is the same AGP version used by the main project.
     */
    internal val agpVersion: String = mainProjectAgpVersion

    /**
     * Version of the Gradle Wrapper used by the generated benchmark project.
     * The default value is the same Gradle version used by the main project.
     */
    internal val gradleVersion = mainProjectGradleVersion

    /**
     * Path to the Gradle Wrapper properties file used by the main project.
     * It will be copied to the generated benchmark project if possible.
     */
    internal val gradleWrapperPropertiesFiles: File = mainProjectGradleWrapperPropertiesFile

    internal val compilationName = compilation.name

    // Used internally to generate task names including this target and its compilation.
    internal val gradleTaskName: String =
        "${name.replaceFirstChar { it.uppercase(Locale.ROOT) }}${compilation.name.replaceFirstChar { it.uppercase(Locale.ROOT) }}"

    // Create snapshot of data required for the Benchmark template
    internal fun createTemplateConfig(): TemplateConfiguration {
        return TemplateConfiguration(
            jvmTargetName = jvmTarget.name,
            jvmToolchain = jvmTarget.toToolchainVersion(),
            namespace = target.namespace ?: throw GradleException("`namespace` is not set"),
            compileSdk = target.compileSdk ?: throw GradleException("`compileSdk` is not set"),
            minSdk = target.minSdk ?: throw GradleException("`minSdk` is not set"),
            kotlinVersion = kotlinVersion,
            agpVersion = agpVersion,
            gradleWrapperPropertiesFile = gradleWrapperPropertiesFiles,
            gradleVersion = gradleVersion.version,
            sdkDir = sdkDir.orNull,
            profilingMode = profilingMode,
            dryRun = dryRun,
            instrumentationRunnerArguments = instrumentationRunnerArguments.toMap(),
        )
    }

    // Internal Config object that captures values in a way that is safe to use with the configuration cache.
    internal data class TemplateConfiguration(
        val jvmTargetName: String,
        val jvmToolchain: Int,
        val namespace: String,
        val compileSdk: Int,
        val minSdk: Int,
        val kotlinVersion: String,
        val agpVersion: String,
        val gradleWrapperPropertiesFile: File,
        val gradleVersion: String,
        val sdkDir: String?,
        val profilingMode: ProfilingMode,
        val dryRun: Boolean,
        val instrumentationRunnerArguments: Map<String, String>,
    ): Serializable

    private fun JvmTarget.toToolchainVersion(): Int =
        name.split("_").last().toInt()
}


/**
 * Enum describing the available profiling modes for Android benchmarks.
 *
 * [Default] will unset the `androidx.benchmark.profiling.mode` instrumentation argument so
 * the behavior will be determined by the Android Benchmarking library. Normally it means
 * using [MethodTracing].
 *
 * See https://developer.android.com/topic/performance/benchmarking/microbenchmark-profile
 * for more information.
 */
enum class ProfilingMode {
    Default,
    MethodTracing,
    StackSampling,
    None,
}
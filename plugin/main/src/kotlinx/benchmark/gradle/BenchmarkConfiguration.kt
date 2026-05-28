package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.internal.BenchmarksPluginConstants
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import kotlin.text.replaceFirstChar

@KotlinxBenchmarkPluginExperimentalApi
class CustomEngine(
    val name: String,
    val enginePath: Provider<RegularFile>,
    val workingDir: Provider<Directory>? = null,
    val engineArguments: Provider<List<String>>? = null,
)

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

    @KotlinxBenchmarkPluginExperimentalApi
    var customEngine: CustomEngine? = null

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
    var buildType: KotlinJsBinaryMode = KotlinJsBinaryMode.PRODUCTION
}

class WasmBenchmarkTarget
@KotlinxBenchmarkPluginInternalApi
constructor(
    extension: BenchmarksExtension,
    name: String,
    @property:KotlinxBenchmarkPluginInternalApi
    val compilation: KotlinJsIrCompilation
) : BenchmarkTarget(extension, name) {
    var buildType: KotlinJsBinaryMode = KotlinJsBinaryMode.PRODUCTION
}

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

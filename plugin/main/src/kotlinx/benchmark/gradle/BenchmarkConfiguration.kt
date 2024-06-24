package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation

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
    fun capitalizedName() = if (name == "main") "" else name.capitalize()

    @KotlinxBenchmarkPluginInternalApi
    fun prefixName(suffix: String) = if (name == "main") suffix else name + suffix.capitalize()

    @KotlinxBenchmarkPluginInternalApi
    fun reportFileExt(): String = reportFormat?.toLowerCase() ?: "json"
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
    var jmhVersion: String = (extension.project.findProperty("benchmarks_jmh_version") as? String) ?: "1.21"
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

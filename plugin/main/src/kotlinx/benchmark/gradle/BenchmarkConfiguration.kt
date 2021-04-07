package kotlinx.benchmark.gradle

import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*

open class BenchmarkConfiguration(val extension: BenchmarksExtension, val name: String) {
    var iterations: Int? = null
    var warmups: Int? = null
    var iterationTime: Long? = null
    var iterationTimeUnit: String? = null
    var mode: String? = null
    var nativeIterationMode: String? = null // TODO: where should warning about K/N specific of this parameter be shown?
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

    fun capitalizedName() = if (name == "main") "" else name.capitalize()
    fun prefixName(suffix: String) = if (name == "main") suffix else name + suffix.capitalize()
    fun reportFileExt(): String = reportFormat?.toLowerCase() ?: "json"
}

open class BenchmarkTarget(val extension: BenchmarksExtension, val name: String) {
    var workingDir: String? = null
}

abstract class JvmBenchmarkTarget(
    extension: BenchmarksExtension,
    name: String
) : BenchmarkTarget(extension, name) {
    var jmhVersion = (extension.project.findProperty("benchmarks_jmh_version") as? String) ?: "1.21"
}

class JavaBenchmarkTarget(
    extension: BenchmarksExtension,
    name: String,
    val sourceSet: SourceSet
) : JvmBenchmarkTarget(extension, name)

open class KotlinJvmBenchmarkTarget(
    extension: BenchmarksExtension,
    name: String,
    val compilation: KotlinJvmCompilation
) : JvmBenchmarkTarget(extension, name)

class JsBenchmarkTarget(
    extension: BenchmarksExtension,
    name: String,
    val compilation: KotlinJsCompilation
) : BenchmarkTarget(extension, name) {

}

class NativeBenchmarkTarget(
    extension: BenchmarksExtension,
    name: String,
    val compilation: KotlinNativeCompilation
) : BenchmarkTarget(extension, name) {

}

package kotlinx.benchmark.gradle

import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation

open class BenchmarkConfiguration(val extension: BenchmarksExtension, val name: String) {
    var iterations: Int? = null
        set(value) {
            value?.let {
                require(it > 0) { "Invalid iterations: '$it'. Expected a positive integer (e.g., iterations = 5)." }
            }
            field = value
        }

    var warmups: Int? = null
        set(value) {
            value?.let {
                require(it >= 0) { "Invalid warmups: '$it'. Expected a non-negative integer (e.g., warmups = 3)." }
            }
            field = value
        }

    var iterationTime: Long? = null
        set(value) {
            value?.let {
                require(it > 0) { "Invalid iterationTime: '$it'. Expected a positive number (e.g., iterationTime = 300)." }
            }
            field = value
        }

    var iterationTimeUnit: String? = null
        set(value) {
            value?.let {
                val validTimeUnits = setOf("seconds", "s", "microseconds", "us", "milliseconds", "ms", "nanoseconds", "ns", "minutes", "m")
                require(it.toLowerCase() in validTimeUnits) { "Invalid iterationTimeUnit: '$it'. Accepted units: ${validTimeUnits.joinToString(", ")} (e.g., iterationTimeUnit = 'ms')." }
            }
            field = value
        }

    var mode: String? = null
        set(value) {
            value?.let {
                val validModes = setOf("thrpt", "avgt")
                require(it.toLowerCase() in validModes) { "Invalid benchmark mode: '$it'. Accepted modes: ${validModes.joinToString(", ")} (e.g., mode = 'thrpt')." }
            }
            field = value
        }

    var outputTimeUnit: String? = null
        set(value) {
            value?.let {
                val validTimeUnits = setOf("seconds", "s", "microseconds", "us", "milliseconds", "ms", "nanoseconds", "ns", "minutes", "m")
                require(it.toLowerCase() in validTimeUnits) { "Invalid outputTimeUnit: '$it'. Accepted units: ${validTimeUnits.joinToString(", ")} (e.g., outputTimeUnit = 'ns')." }
            }
            field = value
        }

    var reportFormat: String? = null
        set(value) {
            value?.let {
                val validFormats = setOf("json", "csv", "scsv", "text")
                require(it.toLowerCase() in validFormats) { "Invalid report format: '$it'. Accepted formats: ${validFormats.joinToString(", ")} (e.g., reportFormat = 'json')." }
            }
            field = value
        }

    var includes: MutableList<String> = mutableListOf()
    var excludes: MutableList<String> = mutableListOf()
    var params: MutableMap<String, MutableList<Any?>> = mutableMapOf()
    var advanced: MutableMap<String, Any?> = mutableMapOf()

    fun include(pattern: String) {
        require(pattern.isNotBlank()) { "Invalid include pattern: '$pattern'. Pattern must not be blank." }
        includes.add(pattern)
    }

    fun exclude(pattern: String) {
        require(pattern.isNotBlank()) { "Invalid exclude pattern: '$pattern'. Pattern must not be blank." }
        excludes.add(pattern)
    }

    fun param(name: String, vararg value: Any?) {
        require(name.isNotBlank()) { "Invalid param name: '$name'. It must not be blank." }
        require(value.isNotEmpty()) { "Param '$name' has no values. At least one value is required." }
        val values = params.getOrPut(name) { mutableListOf() }
        values.addAll(value)
    }

    fun advanced(name: String, value: Any?) {
        require(name.isNotBlank()) { "Invalid advanced config param: '$name'. It must not be blank." }
        require(value.toString().isNotBlank()) { "Invalid value for param '$name': '$value'. Value should not be blank." }
        
        when (name) {
            "nativeFork" -> {
                val validValues = setOf("perbenchmark", "periteration")
                require(value.toString().toLowerCase() in validValues) {
                    "Invalid value for 'nativeFork': '$value'. Accepted values: ${validValues.joinToString(", ")}."
                }
            }
            "nativeGCAfterIteration" -> require(value is Boolean) {
                "Invalid value for 'nativeGCAfterIteration': '$value'. Expected a Boolean value."
            }
            "jvmForks" -> {
                val intValue = value.toString().toIntOrNull()
                require(intValue != null && intValue >= 0 || value.toString().toLowerCase() == "definedbyjmh") {
                    "Invalid value for 'jvmForks': '$value'. Expected a non-negative integer or 'definedByJmh'."
                }
            }
            "jsUseBridge" -> require(value is Boolean) {
                "Invalid value for 'jsUseBridge': '$value'. Expected a Boolean value."
            }
            else -> throw IllegalArgumentException("Invalid advanced config parameter: '$name'. Accepted parameters: 'nativeFork', 'nativeGCAfterIteration', 'jvmForks', 'jsUseBridge'.")
        }
        
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
    var jmhVersion: String = (extension.project.findProperty("benchmarks_jmh_version") as? String) ?: "1.21"
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

enum class JsBenchmarksExecutor {
    BenchmarkJs,
    BuiltIn
}

class JsBenchmarkTarget(
    extension: BenchmarksExtension,
    name: String,
    val compilation: KotlinJsCompilation
) : BenchmarkTarget(extension, name) {
    var jsBenchmarksExecutor: JsBenchmarksExecutor = JsBenchmarksExecutor.BenchmarkJs
}

class WasmBenchmarkTarget(
    extension: BenchmarksExtension,
    name: String,
    val compilation: KotlinJsIrCompilation
) : BenchmarkTarget(extension, name)

class NativeBenchmarkTarget(
    extension: BenchmarksExtension,
    name: String,
    val compilation: KotlinNativeCompilation
) : BenchmarkTarget(extension, name)
package org.jetbrains.gradle.benchmarks

import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*

open class BenchmarkConfiguration(val extension: BenchmarksExtension, val name: String) {
    var iterations: Int? = null
    var iterationTime: Long? = null
    var iterationTimeUnit: String? = null
    var mode: String? = null
    var outputTimeUnit: String? = null
    
    var includes: MutableList<String> = mutableListOf()
    var excludes: MutableList<String> = mutableListOf()
    var params: MutableMap<String, Any?> = mutableMapOf()

    fun include(pattern: String) {
        includes.add(pattern)
    }

    fun exclude(pattern: String) {
        excludes.add(pattern)
    }

    fun param(name: String, value: Any?) {
        params[name] = value
    }

    fun capitalizedName() = if (name == "main") "" else name.capitalize()
    fun prefixName(suffix: String) = if (name == "main") suffix else name + suffix.capitalize()
}

open class BenchmarkTarget(val extension: BenchmarksExtension, val name: String) {
    var workingDir: String? = null
}

class BenchmarkConfigurationDefaults {
    var iterations = 10 // times
    var iterationTime = 1L // seconds
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
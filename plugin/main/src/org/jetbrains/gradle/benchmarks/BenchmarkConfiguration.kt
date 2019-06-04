package org.jetbrains.gradle.benchmarks

import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*

open class BenchmarkConfiguration(val extension: BenchmarksExtension, val name: String) {
    var iterations: Int? = null
    var iterationTime: Long? = null
    var iterationTimeUnit: String? = null
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

    // TODO: this is error prone. User should use the mutable variables above. Plugin code should use these methods
    // Basically, we want external properties, such as `-P` to override what's in build script
    // We also want to use defaults if per configuration setting is missing 
    fun iterations(): Int {
        val externalSetting = extension.project.findProperty("benchmarks_iterations") as? String
        if (externalSetting != null)
            return externalSetting.toInt()
        return iterations ?: extension.defaults.iterations
    }

    fun iterationTime(): Long {
        val externalSetting = extension.project.findProperty("benchmarks_iterationTime") as? String
        if (externalSetting != null)
            return externalSetting.toLong()
        return iterationTime ?: extension.defaults.iterationTime
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
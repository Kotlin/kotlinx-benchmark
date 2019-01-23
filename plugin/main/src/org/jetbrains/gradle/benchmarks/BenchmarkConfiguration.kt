package org.jetbrains.gradle.benchmarks

import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*

open class BenchmarkConfiguration(val extension: BenchmarksExtension, val name: String) { 
    var iterations: Int? = null
    var iterationTime: Long? = null

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
}

class BenchmarkConfigurationDefaults {
    var iterations = 10 // times
    var iterationTime = 1000L // ms
}

class JavaBenchmarkConfiguration(
    extension: BenchmarksExtension,
    name: String,
    val sourceSet: SourceSet
) :
    BenchmarkConfiguration(extension, name) {

    var jmhVersion = (extension.project.findProperty("benchmarks_jmh_version") as? String) ?: "1.21"
}

open class JvmBenchmarkConfiguration(
    extension: BenchmarksExtension,
    name: String,
    val compilation: KotlinJvmCompilation
) : BenchmarkConfiguration(extension, name) {

    var jmhVersion = (extension.project.findProperty("benchmarks_jmh_version") as? String) ?: "1.21"
}

class JsBenchmarkConfiguration(
    extension: BenchmarksExtension,
    name: String,
    val compilation: KotlinJsCompilation
) : BenchmarkConfiguration(extension, name) {

}

class NativeBenchmarkConfiguration(
    extension: BenchmarksExtension,
    name: String,
    val compilation: KotlinNativeCompilation
) : BenchmarkConfiguration(extension, name) {

}
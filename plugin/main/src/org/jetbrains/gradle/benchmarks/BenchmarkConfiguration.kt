package org.jetbrains.gradle.benchmarks

import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*

open class BenchmarkConfiguration(val extension: BenchmarksExtension, val name: String) {
    var iterations = ((extension.project.findProperty("benchmarks_iterations") as? String) ?: "10").toInt()
    var iterationTime = ((extension.project.findProperty("benchmarks_iterationTime") as? String) ?: "1000").toInt()
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
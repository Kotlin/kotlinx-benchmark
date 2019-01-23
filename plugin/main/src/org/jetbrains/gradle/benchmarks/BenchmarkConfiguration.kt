package org.jetbrains.gradle.benchmarks

import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*

open class BenchmarkConfiguration(val extension: BenchmarksExtension, val name: String)

class JavaBenchmarkConfiguration(
    extension: BenchmarksExtension,
    name: String,
    val sourceSet: SourceSet
) :
    BenchmarkConfiguration(extension, name) {
    var jmhVersion = "1.21"

}

open class JvmBenchmarkConfiguration(
    extension: BenchmarksExtension,
    name: String,
    val compilation: KotlinJvmCompilation
) : BenchmarkConfiguration(extension, name) {
    var jmhVersion = "1.21"
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
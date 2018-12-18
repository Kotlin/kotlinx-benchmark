package org.jetbrains.gradle.benchmarks

import org.gradle.api.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*

fun Project.configureKotlinMultiplatform(
    extension: BenchmarksExtension,
    config: BenchmarkConfiguration,
    multiplatform: KotlinMultiplatformExtension
) {
    project.logger.info("Configuring benchmarks for '${config.name}' using Multiplatform Kotlin")

    val compilations = multiplatform.targets.flatMap { it.compilations }

    // TODO: Find a way to get something like compilation base name
    // we wan to refernce jvm:main as just `jvm`, and jvm:test as `jvmTest`
    
    val compilation = compilations.singleOrNull { it.apiConfigurationName.removeSuffix("Api") == config.name }
    if (compilation == null) {
        logger.warn("Warning: Cannot find a benchmark compilation '${config.name}', ignoring.")
        return // ignore
    }

    when (compilation) {
        is KotlinJvmCompilation -> {
            processJvmCompilation(extension, config, compilation)
        }
        is KotlinJsCompilation -> {
            processJsCompilation(extension, config, compilation)
        }
    }
}

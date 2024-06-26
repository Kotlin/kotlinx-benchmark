import kotlinx.validation.ExperimentalBCVApi

plugins {
    id("kotlinx.benchmarks_build.conventions.base")

    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false

    alias(libs.plugins.kotlinx.binaryCompatibilityValidator)
}

apiValidation {
    nonPublicMarkers += "kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi"

    @OptIn(ExperimentalBCVApi::class)
    klib {
        enabled = true
    }
}

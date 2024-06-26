plugins {
    id("kotlinx.benchmarks_build.conventions.base")

    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
}

import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    <<DISABLE_KOTLIN_ANDROID_PLUGIN>>alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.androidx.benchmark)
}

kotlin {
    jvmToolchain(<<JVM_TOOLCHAIN>>)
    compilerOptions {
        jvmTarget = <<JVM_TARGET>>
    }
}

android {
    namespace = "<<NAMESPACE>>.benchmark"
    compileSdk = <<ANDROID_COMPILE_SDK>>
    defaultConfig {
        minSdk = <<ANDROID_MIN_SDK>>
        testInstrumentationRunner = "androidx.benchmark.junit4.AndroidBenchmarkRunner"
        testInstrumentationRunnerArguments["androidx.benchmark.output.enable"] = "true"
        <<TEST_INSTRUMENTATION_RUNNER_ARGUMENTS>>
    }
    testBuildType = "release"
    buildTypes {
        getByName("release") {
            // The androidx.benchmark plugin configures release buildType with proper settings, such as:
            // - disables code coverage
            // - adds CPU clock locking task
            // - signs release buildType with debug signing config
            // - copies benchmark results into build/outputs/connected_android_test_additional_output folder
        }
    }
}

dependencies {
    androidTestImplementation(files("<<BENCHMARKED_AAR_ABSOLUTE_PATH>>"))
<<ADDITIONAL_DEPENDENCIES>>
    androidTestImplementation(libs.androidx.benchmark)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.junit)
}
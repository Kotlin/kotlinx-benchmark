import kotlinx.benchmark.gradle.JvmBenchmarkTarget

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.allopen") version "2.0.20"
    id("org.jetbrains.kotlinx.benchmark")
}

// how to apply plugin to a specific source set?
allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

sourceSets {
    create("benchmarks")
}

sourceSets.configureEach {
    kotlin.setSrcDirs(listOf(file("$name/src")))
    java.setSrcDirs(listOf(file("$name/src")))
    resources.setSrcDirs(listOf(file("$name/resources")))
}

kotlin {
    /*
    Associate the benchmarks with the main compilation.
    This will:
    1. Allow 'benchmarks' to see all internals of 'main'
    2. Forward all dependencies from 'main' to be also visible in 'benchmarks'
     */
    target.compilations.getByName("benchmarks")
        .associateWith(target.compilations.getByName("main"))
}

// Add dependency to the benchmark runtime
dependencies {
    "benchmarksImplementation"(project(":kotlinx-benchmark-runtime"))
}


// Configure benchmark
benchmark {
    targets {
        register("benchmarks") {
            if (this is JvmBenchmarkTarget) {
                jmhVersion = "1.21"
            }
        }
    }
}

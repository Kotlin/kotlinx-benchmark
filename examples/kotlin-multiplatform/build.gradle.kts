@file:OptIn(ExperimentalWasmDsl::class)

import kotlinx.benchmark.gradle.*
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl


plugins {
    kotlin("multiplatform")
    kotlin("plugin.allopen") version "2.0.20"
    id("org.jetbrains.kotlinx.benchmark")
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

kotlin {
    jvm {
        compilations.create("benchmark") { associateWith(this@jvm.compilations.getByName("main")) }
    }
    js {
        nodejs()
        val mainCompilation = compilations.getByName("main")
        compilations.create("defaultExecutor") { associateWith(mainCompilation) }
        compilations.create("builtInExecutor") { associateWith(mainCompilation) }
    }
    wasmJs { nodejs() }

    // Native targets
    macosX64()
    macosArm64()
    linuxX64()
    mingwX64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":kotlinx-benchmark-runtime"))
            }
        }

        jvmMain {}

        wasmJsMain {}

        val jsMain by getting

        getByName("jsDefaultExecutor") {
            dependsOn(jsMain)
        }

        getByName("jsBuiltInExecutor") {
            dependsOn(jsMain)
        }

        nativeMain {}
    }
}

// Configure benchmark
benchmark {
    configurations {
        named("main") { // --> jvmBenchmark, jsBenchmark, <native target>Benchmark, benchmark
            iterations = 5 // number of iterations
            iterationTime = 300
            iterationTimeUnit = "ms"
            advanced("jvmForks", 3)
            advanced("jsUseBridge", true)
        }

        create("params") {
            iterations = 5 // number of iterations
            iterationTime = 300
            iterationTimeUnit = "ms"
            include("ParamBenchmark")
            param("data", 5, 1, 8)
            param("unused", 6, 9)
        }

        create("fast") { // --> jvmFastBenchmark, jsFastBenchmark, <native target>FastBenchmark, fastBenchmark
            include("Common")
            exclude("long")
            iterations = 5
            iterationTime = 300 // time in ms per iteration
            iterationTimeUnit = "ms" // time in ms per iteration
            advanced("nativeGCAfterIteration", true)
        }

        create("csv") {
            include("Common")
            exclude("long")
            iterations = 1
            iterationTime = 300
            iterationTimeUnit = "ms"
            reportFormat = "csv" // csv report format
        }

        create("fork") {
            include("CommonBenchmark")
            iterations = 5
            iterationTime = 300
            iterationTimeUnit = "ms"
            advanced("jvmForks", "definedByJmh") // see README.md for possible "jvmForks" values
            advanced("nativeFork", "perIteration") // see README.md for possible "nativeFork" values
        }
    }

    // Setup configurations
    targets {
        // This one matches the target name, e.g. 'jvm', 'js',
        // and registers its 'main' compilation, so 'jvm' registers 'jvmMain'
        register("jvm") {
            this as JvmBenchmarkTarget
            jmhVersion = "1.37"
        }
        // This one matches the source set name, e.g. 'jvmMain', 'jvmTest', etc
        // and register the corresponding compilation (here the 'benchmark' compilation declared in the 'jvm' target)
        register("jvmBenchmark") {
            this as JvmBenchmarkTarget
            jmhVersion = "1.37"
        }
        register("jsDefaultExecutor")
        register("jsBuiltInExecutor") {
            this as JsBenchmarkTarget
            jsBenchmarksExecutor = JsBenchmarksExecutor.BuiltIn
        }
        register("wasmJs")

        // Native targets
        register("macosX64")
        register("macosArm64")
        register("linuxX64")
        register("mingwX64")
    }
}

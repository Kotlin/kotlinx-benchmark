import kotlinx.benchmark.gradle.*
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import kotlinx.benchmark.gradle.JsBenchmarksExecutor

plugins {
    kotlin("multiplatform")
    kotlin("plugin.allopen") version "1.9.0"
    id("org.jetbrains.kotlinx.benchmark")
}

// how to apply plugin to a specific source set?
allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

kotlin {
    jvm {
        compilations.create("benchmark") { associateWith(compilations.getByName("main")) }
    }
    js("jsIr", IR) { nodejs() }
    js("jsIrBuiltIn", IR) { nodejs() }
    wasm("wasmJs") { d8() }
    if (HostManager.host == KonanTarget.MACOS_X64) macosX64("native")
    if (HostManager.host == KonanTarget.MACOS_ARM64) macosArm64("native")
    if (HostManager.hostIsLinux) linuxX64("native")
    if (HostManager.hostIsMingw) mingwX64("native")

    sourceSets.all {
        languageSettings {
            progressiveMode = true
        }
    }

    sourceSets {
        getByName("commonMain") {
            dependencies {
                implementation(project(":kotlinx-benchmark-runtime"))
            }
        }

        getByName("jvmMain") {}

        getByName("wasmJsMain") {}


        val jsMain by creating

        getByName("jsIrMain") {
            dependsOn(jsMain)
        }

        getByName("jsIrBuiltInMain") {
            dependsOn(jsMain)
        }

        getByName("nativeMain") {
            dependsOn(getByName("commonMain"))
        }
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
        // This one matches target name, e.g. 'jvm', 'js',
        // and registers its 'main' compilation, so 'jvm' registers 'jvmMain'
        register("jvm") {
            this as JvmBenchmarkTarget
            jmhVersion = "1.21"
        }
        // This one matches source set name, e.g. 'jvmMain', 'jvmTest', etc
        // and register the corresponding compilation (here the 'benchmark' compilation declared in the 'jvm' target)
        register("jvmBenchmark") {
            this as JvmBenchmarkTarget
            jmhVersion = "1.21"
        }
        register("jsIr")
        register("jsIrBuiltIn") {
            this as JsBenchmarkTarget
            jsBenchmarksExecutor = JsBenchmarksExecutor.BuiltIn
        }
        register("wasmJs")
        register("native")
    }
}
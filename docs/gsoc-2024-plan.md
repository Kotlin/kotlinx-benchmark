# GSoC 2024 plan

**Glossary**:
* Benchmark project - The project in which benchmarks are written using `kotlinx-benchmark` annotations.


## User experience

From the user point of view, adding an Android target to their multiplatform benchmark project should not change UX.
Here is how the benchmark project build script would appear:
```kotlin
plugins {
    id("org.jetbrains.kotlin.multiplatform") version "x.y.z"
    id("com.android.library") version "a.b.c"

    /*...*/
}


kotlin {
    jvm { /*...*/ }
    js { /*...*/ }
    macosArm64 { /*...*/ }

    // adding android target
    androidTarget { /*...*/ }
}

android {
    // configure the android app module
}

benchmark {
    configurations { /*...*/ }

    targets {
        register("jvm")
        register("js")
        register("macosArm64")

        // register android as a benchmark target
        register("android") // or androidTarget
    }
}
```

When running benchmarks for the Android target, all benchmarks defined in the `commonMain` and `androidMain` source sets are run.


## Technical implementation

### High-level vision

When an Android target is registered for benchmarking, the `kotlinx-benchmark` plugin
1. Detects the android target.
2. Takes a compilation output of the target. 
   1. Perhaps the release compilation?
3. Retrieves data related to the benchmark annotations.
4. Generates a new (empty) Android project.
   1. Perhaps copies a template android project to the `build` directory of the benchmark project?
5. Generates code that uses the `androidx-benchmark` Microbenchmark library to benchmark the original benchmark methods according to the configuration options and annotation data.
   1. The generated code should be organized as Android instrumented tests?
6. The generated code (or tests) is then run on an Android device, and the results are reported. 
   1. Perhaps Android Studio should be running and a device or emulator should be connected during benchmark run?
7. The progress of the benchmark run is reported in the console, and the final results are written to a file.


### Useful information

* It is important to note that `kotlinx-benchmark` is a microbenchmarking library. That is, it measures the performance of a small and isolated piece of code rather than the performance of an entire system or application.
* [Setting up a Kotlin Multiplatform project for benchmarking](../README.md).
* [Writing benchmarks using kotlinx-benchmark](writing-benchmarks.md).
* [The Gradle tasks created by the kotlinx-benchmark plugin that run benchmarks for a particular target](tasks-overview.md).
* [Detection of the registered targets for benchmarking](https://github.com/Kotlin/kotlinx-benchmark/blob/master/plugin/main/src/kotlinx/benchmark/gradle/BenchmarksExtension.kt#L48-L116).
* [Processing each registered benchmark target](https://github.com/Kotlin/kotlinx-benchmark/blob/master/plugin/main/src/kotlinx/benchmark/gradle/BenchmarksPlugin.kt#L95-L107). e.g., for a Kotlin/Native target:
    * [Creation of the Gradle task for generating source code](https://github.com/Kotlin/kotlinx-benchmark/blob/619f67d4f52dcfec7b6d4eecd33e19aa01de55da/plugin/main/src/kotlinx/benchmark/gradle/NativeMultiplatformTasks.kt#L36).
    * [The class that retrieve annotation data and generates code](https://github.com/Kotlin/kotlinx-benchmark/blob/619f67d4f52dcfec7b6d4eecd33e19aa01de55da/plugin/main/src/kotlinx/benchmark/gradle/SuiteSourceGenerator.kt#L51).
    * [Creation of the Gradle task for running the generated source code](https://github.com/Kotlin/kotlinx-benchmark/blob/619f67d4f52dcfec7b6d4eecd33e19aa01de55da/plugin/main/src/kotlinx/benchmark/gradle/NativeMultiplatformTasks.kt#L110).
* An example of transforming benchmarks from [kotlinx-benchmark](https://github.com/Kotlin/kotlinx-io/blob/android-benchmarks/benchmarks/src/commonMain/kotlin/BufferOps.kt)
  to [androidx.benchmark](https://github.com/Kotlin/kotlinx-io/blob/android-benchmarks/benchmarks-android/src/androidTest/java/kotlinx/io/benchmark/android/BufferOps.kt).
  * This is just for reference and exploration purposes. Our implementation may generate code following a different pattern.

### Proposed steps for implementing the project

1. Detect when an Android target is registered for benchmarking.
2. Look into the target compilations, explore their output.
   1. Which compilation is the best for processing and generating code?
3. Determine how the output can be deserialized for processing.
   1. How can the annotation data be retrieved?
4. Generate any code inside an existing android application project that calls one of the benchmark functions.
   1. Ensure the dependency on the compilation output is correctly resolved.
   2. Ensure the generated code compiles and runs.
5. Generate code that uses the `androidx-benchmark` library to run a benchmark function.
   1. Not necessarily according to the annotations.
   2. Ensure the generated code compiles and runs.
6. Generate code for running all benchmark functions according to the relevant annotations.
   1. Can all `kotlinx-benchmark` annotations be mapped to `androidx-benchmark` configurations?
7. Take configuration options of plugin extension into account.
8. What Android specific configurations options should be introduced?
   1. Ahead-of-time vs Just-in-time compilation.

## Expected outcomes

### First part (steps 1 through 5): Exploratory phase

The mentee will explore and learn about:
* The `kotlinx-benchmark` library, including its plugin, annotations and runtime.
* Kotlin Multiplatform targets and their support in `kotlinx-benchmark`.
* Android compilations and their outputs.
* Deserialization of compiled benchmark code.
* Retrieval of metadata from benchmark annotations.
* Generation of Kotlin code, possibly using the `kotlinpoet` library.
* Execution of the generated code on an Android emulator or a connected device, including detection of the device or emulator.
* Execution of a Gradle task of a separate project, potentially through a shell command.
* The `androidx.benchmark` library and its capabilities.

### Second part (steps 6 through 8): Implementation phase

The learnings from the first part will be applied in the actual implementation of the GSoC project.

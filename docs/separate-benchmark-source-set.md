# Setting Up a Separate Source Set for Benchmarks

This guide will walk you through the process of establishing a dedicated source set for benchmarks within your Kotlin project. 
This approach is especially beneficial when you are integrating benchmarks into an existing project. 
Here are a couple of advantages of doing so:

1. **Flexibility**: Setting up a separate source set allows you to manage your benchmarking code independently. You can compile, test, and run benchmarks without impacting your main source code.

2. **Organization**: It helps maintain a clean and organized project structure. Segregating benchmarks from the main code makes it easier to navigate and locate specific code segments.

## Step-by-step Setup Guide

Below are the step-by-step instructions to set up a separate source set for benchmarks in both Kotlin Multiplatform and Kotlin/JVM projects:

### Kotlin Multiplatform Project

Follow these steps to set up a separate source set for benchmarks:

1. **Define New Compilation**

    Start by creating a new compilation in your target of choice (e.g. jvm, js, native, wasm etc.). 
    In this example, we're associating the new compilation `benchmark` with the `main` compilation of the `jvm` target. 
    This association allows the benchmark compilation to access the internal API of the main compilation, 
    which is particularly useful when benchmarks need to measure the performance of specific components 
    or functionalities within the main codebase.

    ```kotlin
    // build.gradle.kts
    kotlin {
        jvm {
            compilations.create("benchmark") {
                associateWith(this@jvm.compilations.getByName("main"))
            }
        }
    }
    ```

2. **Register Benchmark Compilation**

    Register your new benchmark compilation using its default source set name. 
    In this instance, `jvmBenchmark` is the name of the default source set of the `benchmark` compilation.

    ```kotlin
    // build.gradle.kts
    benchmark {
        targets {
            register("jvmBenchmark")
        }
    }
    ```

3. **Add Benchmarks**

    Place your benchmark code into the default source set of the benchmark compilation. 
    The default source set can also depend on other source sets containing benchmarks. 
    This way you can share benchmarks between multiple benchmark compilations. 
    Refer to our [writing benchmarks guide](writing-benchmarks.md) for a comprehensive guide on writing benchmarks.

For additional information, refer to the [Kotlin documentation on creating a custom compilation](https://kotlinlang.org/docs/multiplatform-configure-compilations.html#create-a-custom-compilation).
and the [documentation on associating compiler tasks](https://kotlinlang.org/docs/gradle-configure-project.html#associate-compiler-tasks).
[Here is a sample Kotlin Multiplatform project](/examples/kotlin-multiplatform) with a separate compilation for benchmarks.

### Kotlin/JVM Project

Set up a separate benchmark source set by following these simple steps:

1. **Define Source Set**

    Begin by defining a new source set. We'll use `benchmark` as the name for the source set.

    ```kotlin
    // build.gradle.kts
    sourceSets {
        create("benchmark")
    }
    ```

2. **Propagate Dependencies**

    Next, propagate dependencies and output from the `main` source set to your `benchmark` source set. 
    This ensures the `benchmark` source set has access to classes and resources from the `main` source set.

    ```kotlin
    // build.gradle.kts
    dependencies {
        add("benchmarkImplementation", sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath)
    }
    ```

    You can also add output and `compileClasspath` from `sourceSets.test` in the same way 
    if you wish to reuse some of the test infrastructure.

3. **Register Benchmark Source Set**

    Register your benchmark source set. This informs the kotlinx-benchmark tool 
    that benchmarks reside within this source set and need to be executed accordingly.

    ```kotlin
    // build.gradle.kts
    benchmark {
        targets { 
            register("benchmark")
        }
    }
    ```

4. **Add Benchmarks**

   Place your benchmark code into the benchmark source set.
   Refer to our [writing benchmarks guide](writing-benchmarks.md) for a comprehensive guide on writing benchmarks.

[Here is a sample Kotlin/JVM project](/examples/kotlin) with custom source set for benchmarks.
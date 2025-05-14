# kotlinx-benchmark

[![Kotlin Alpha](https://kotl.in/badges/alpha.svg)](https://kotlinlang.org/docs/components-stability.html)
[![JetBrains incubator project](https://jb.gg/badges/incubator.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![GitHub license](https://img.shields.io/github/license/kotlin/kotlinx-benchmark)](LICENSE)
[![Build status](https://teamcity.jetbrains.com/guestAuth/app/rest/builds/buildType:(id:KotlinTools_KotlinxBenchmark_Build_All)/statusIcon.svg)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=KotlinTools_KotlinxBenchmark_Build_All)
[![Maven Central](https://img.shields.io/maven-central/v/org.jetbrains.kotlinx/kotlinx-benchmark-runtime.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/org.jetbrains.kotlinx/kotlinx-benchmark-runtime)
[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v?label=Gradle%20Plugin&metadataUrl=https://plugins.gradle.org/m2/org/jetbrains/kotlinx/kotlinx-benchmark-plugin/maven-metadata.xml)](https://plugins.gradle.org/plugin/org.jetbrains.kotlinx.benchmark)

`kotlinx-benchmark` is a toolkit for running benchmarks for multiplatform code written in Kotlin.
It is designed to work with Kotlin/JVM, Kotlin/JS, Kotlin/Native, and Kotlin/WasmJs (experimental) targets.

To get started, ensure you're using Kotlin 2.0.0 or newer and Gradle 7.4 or newer.
However, because the Kotlin/WasmJs target is experimental and in an active development phase, it guarantees support
only for the specific Kotlin version used to build the library. For the latest version, this is Kotlin 2.0.20.

## Features

- Low noise and reliable results
- Statistical analysis
- Detailed performance reports

## Table of contents

<!--- TOC -->

- [Setting Up a Kotlin Multiplatform Project for Benchmarking](#setting-up-a-kotlin-multiplatform-project-for-benchmarking)
  - [Target-specific configurations](#target-specific-configurations)
    - [Kotlin/JVM](#kotlinjvm)
    - [Kotlin/JS](#kotlinjs)
    - [Kotlin/Native](#kotlinnative)
    - [Kotlin/Wasm](#kotlinwasm)
  - [Writing Benchmarks](#writing-benchmarks)
  - [Running Benchmarks](#running-benchmarks)
  - [Benchmark Configuration Profiles](#benchmark-configuration-profiles)
  - [Separate source set for benchmarks](#separate-source-set-for-benchmarks)
- [Examples](#examples)
- [Contributing](#contributing)

<!--- END -->

- **Useful guides**
  - [Writing Benchmarks](docs/writing-benchmarks.md)
  - [Configuring Benchmarks Execution](docs/configuration-options.md)
  - [Setting Up Kotlin/JVM and Java Projects for Benchmarking](docs/kotlin-jvm-project-setup.md)
  - [Setting Up a Separate Source Set for Benchmarks](docs/separate-benchmark-source-set.md)
  - [Overview of Tasks Provided by kotlinx-benchmark Gradle Plugin](docs/tasks-overview.md)

## Setting Up a Kotlin Multiplatform Project for Benchmarking

To configure a Kotlin Multiplatform project for benchmarking, follow the steps below. 
If you want to benchmark only Kotlin/JVM and Java code, you may refer to our [comprehensive guide](docs/kotlin-jvm-project-setup.md) 
dedicated to setting up benchmarking in those specific project types.

<details open><summary>Kotlin DSL</summary>

1.  **Applying Benchmark Plugin**: Apply the benchmark plugin.

    ```kotlin
    // build.gradle.kts
    plugins {
        id("org.jetbrains.kotlinx.benchmark") version "0.4.14"
    }
    ```

2.  **Specifying Plugin Repository**: Ensure you have the Gradle Plugin Portal for plugin lookup in the list of repositories:

    ```kotlin
    // settings.gradle.kts
    pluginManagement {
        repositories {
            gradlePluginPortal()
        }
    }
    ```

3.  **Adding Runtime Dependency**: Next, add the `kotlinx-benchmark-runtime` dependency to the common source set:

    ```kotlin
    // build.gradle.kts
    kotlin {
        sourceSets {
            commonMain {
                dependencies {
                    implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.14")
                }
            }
        }
    }
    ```

4.  **Specifying Runtime Repository**: Ensure you have `mavenCentral()` for dependencies lookup in the list of repositories:

    ```kotlin
    // build.gradle.kts
    repositories {
        mavenCentral()
    }
    ```

</details>

<details><summary>Groovy DSL</summary>

1.  **Applying Benchmark Plugin**: Apply the benchmark plugin.

    ```groovy
    // build.gradle
    plugins {
        id 'org.jetbrains.kotlinx.benchmark' version '0.4.14'
    }
    ```

2.  **Specifying Plugin Repository**: Ensure you have the Gradle Plugin Portal for plugin lookup in the list of repositories:

    ```groovy
    // settings.gradle
    pluginManagement {
        repositories {
            gradlePluginPortal()
        }
    }
    ```

3.  **Adding Runtime Dependency**: Next, add the `kotlinx-benchmark-runtime` dependency to the common source set:

    ```groovy
    // build.gradle
    kotlin {
        sourceSets {
            commonMain {
                dependencies {
                    implementation 'org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.14'
                }
            }
        }
    }
    ```

4.  **Specifying Runtime Repository**: Ensure you have `mavenCentral()` for dependencies lookup in the list of repositories:

    ```groovy
    // build.gradle
    repositories {
        mavenCentral()
    }
    ```

</details>

### Target-specific configurations

To run benchmarks on a platform ensure your Kotlin Multiplatform project targets that platform.
For different platforms, there may be distinct requirements and settings that need to be configured.
The guide below contains the steps needed to configure each supported platform for benchmarking.

#### Kotlin/JVM

To run benchmarks in Kotlin/JVM:
1.  Create a JVM target:

    ```kotlin
    // build.gradle.kts
    kotlin {
        jvm()
    }
    ```

2.  Register `jvm` as a benchmark target:

    ```kotlin
    // build.gradle.kts
    benchmark {
        targets { 
            register("jvm")
        }
    }
    ```

3.  Apply [allopen plugin](https://kotlinlang.org/docs/all-open-plugin.html) to ensure your benchmark classes and methods are `open`.

    ```kotlin
    // build.gradle.kts
    plugins {
        kotlin("plugin.allopen") version "2.0.20"
    }

    allOpen {
        annotation("org.openjdk.jmh.annotations.State")
    }
    ```

    <details><summary><b>Explanation</b></summary>

    Assume that you've annotated each of your benchmark classes with `@State(Scope.Benchmark)`:

    ```kotlin
    // MyBenchmark.kt
    @State(Scope.Benchmark)
    class MyBenchmark {
        // Benchmarking-related methods and variables
        @Benchmark
        fun benchmarkMethod() {
            // benchmarking logic
        }
    }
    ```

    In Kotlin, classes are `final` by default, which means they can't be overridden.
    This conflicts with the Java Microbenchmark Harness (JMH) operation, which `kotlinx-benchmark` uses under the hood for running benchmarks on JVM.
    JMH requires benchmark classes and methods to be `open` to be able to generate subclasses and conduct the benchmark.

    This is where the `allopen` plugin comes into play. With the plugin applied, any class annotated with `@State` is treated as `open`, which allows JMH to work as intended:

    ```kotlin
    // build.gradle.kts
    plugins {
        kotlin("plugin.allopen") version "2.0.20"
    }

    allOpen {
        annotation("org.openjdk.jmh.annotations.State")
    }
    ```

    This configuration ensures that your `MyBenchmark` class and its `benchmarkMethod` function are treated as `open`.

    </details>

    You can alternatively mark your benchmark classes and methods `open` manually, but using the `allopen` plugin enhances code maintainability.

#### Kotlin/JS

To run benchmarks in Kotlin/JS:
1.  Create a JS target with Node.js execution environment:

    ```kotlin
    // build.gradle.kts
    kotlin {
        js { 
            nodejs() 
        }
    }
    ```

2.  Register `js` as a benchmark target:

    ```kotlin
    // build.gradle.kts
    benchmark {
        targets {
            register("js")
        }
    }
    ```

#### Kotlin/Native

To run benchmarks in Kotlin/Native:
1.  Create a Native target:

    ```kotlin
    // build.gradle.kts
    kotlin {
        linuxX64()
    }
    ```

2.  Register `linuxX64` as a benchmark target:

    ```kotlin
    // build.gradle.kts
    benchmark {
        targets {
            register("linuxX64")
        }
    }
    ```
    
It is possible to register multiple native targets. However, benchmarks can be executed only for the host target.
This library supports all [targets supported by the Kotlin/Native compiler](https://kotlinlang.org/docs/native-target-support.html). 

#### Kotlin/Wasm

To run benchmarks in Kotlin/Wasm:
1.  Create a Wasm target with Node.js execution environment:

    ```kotlin
    // build.gradle.kts
    kotlin {
        wasm { 
            nodejs() 
        }
    }
    ```

2.  Register `wasm` as a benchmark target:

    ```kotlin
    // build.gradle.kts
    benchmark {
        targets {
            register("wasm")
        }
    }
    ```

Note: Kotlin/Wasm is an experimental compilation target for Kotlin. It may be dropped or changed at any time. Refer to 
[Kotlin/Wasm documentation](https://kotlinlang.org/docs/wasm-overview.html) for up-to-date information about the target stability.

### Writing Benchmarks

After setting up your project and configuring targets, you can start writing benchmarks.
As an example, let's write a simplified benchmark that tests how fast we can add up numbers in an `ArrayList`:

1. **Create Benchmark Class**: Create a class in your source set where you'd like to add the benchmark. Annotate this class with `@State(Scope.Benchmark)`.

    ```kotlin
    @State(Scope.Benchmark)
    class MyBenchmark {

    }
    ```

2. **Set Up Variables**: Define variables needed for the benchmark.

    ```kotlin
    private val size = 10
    private val list = ArrayList<Int>()
    ```

3. **Initialize Resources**: Within the class, you can define any setup or teardown methods using `@Setup` and `@TearDown` annotations respectively. These methods will be executed before and after the entire benchmark run.

    ```kotlin
    @Setup
    fun prepare() {
        for (i in 0..<size) {
            list.add(i)
        }
    }

    @TearDown
    fun cleanup() {
        list.clear()
    }
    ```

4. **Define Benchmark Methods**: Next, create methods that you would like to be benchmarked within this class and annotate them with `@Benchmark`.

    ```kotlin
    @Benchmark
    fun benchmarkMethod(): Int {
        return list.sum()
    }
    ```

Your final benchmark class will look something like this:

```kotlin
import kotlinx.benchmark.*

@State(Scope.Benchmark)
class MyBenchmark {
    private val size = 10
    private val list = ArrayList<Int>()

    @Setup
    fun prepare() {
        for (i in 0..<size) {
            list.add(i)
        }
    }

    @TearDown
    fun cleanup() {
        list.clear()
    }

    @Benchmark
    fun benchmarkMethod(): Int {
        return list.sum()
    }
}
```

Note: Benchmark classes located in the common source set will be run in all platforms, while those located in a platform-specific source set will be run only in the corresponding platform.

See [writing benchmarks](docs/writing-benchmarks.md) for a complete guide for writing benchmarks.

### Running Benchmarks

To run your benchmarks in all registered platforms, run `benchmark` Gradle task in your project.
To run only on a specific platform, run `<target-name>Benchmark`, e.g., `jvmBenchmark`.

For more details about the tasks created by the `kotlinx-benchmark` plugin, refer to [this guide](docs/tasks-overview.md).

### Benchmark Configuration Profiles

The `kotlinx-benchmark` library provides the ability to create multiple configuration profiles. The `main` configuration is already created by the toolkit.
Additional profiles can be created as needed in the `configurations` section of the `benchmark` block:

```kotlin
// build.gradle.kts
benchmark {
    configurations {
        named("main") {
            warmups = 20
            iterations = 10
            iterationTime = 3
            iterationTimeUnit = "s"
        }
        register("smoke") {
            include("<pattern of fully qualified name>")
            warmups = 5
            iterations = 3
            iterationTime = 500
            iterationTimeUnit = "ms"
        }
    }
}
```  

Refer to our [comprehensive guide](docs/configuration-options.md) to learn about configuration options and how they affect benchmark execution.

### Separate source set for benchmarks

Often you want to have benchmarks in the same project, but separated from main code, much like tests.
Refer to our [detailed documentation](docs/separate-benchmark-source-set.md) on configuring your project to set up a separate source set for benchmarks.

## Examples

To help you better understand how to use the `kotlinx-benchmark` library, we've provided an [examples](examples) subproject. 
These examples showcase various use cases and offer practical insights into the library's functionality.

## Contributing

We welcome contributions to `kotlinx-benchmark`! If you want to contribute, please refer to our [Contribution Guidelines](CONTRIBUTING.md).

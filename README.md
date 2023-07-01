# kotlinx-benchmark

[![Kotlin Alpha](https://kotl.in/badges/alpha.svg)](https://kotlinlang.org/docs/components-stability.html)
[![JetBrains incubator project](https://jb.gg/badges/incubator.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)
[![Build status](<https://teamcity.jetbrains.com/guestAuth/app/rest/builds/buildType:(id:KotlinTools_KotlinxCollectionsImmutable_Build_All)/statusIcon.svg>)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=KotlinTools_KotlinxBenchmark_Build_All)
[![Maven Central](https://img.shields.io/maven-central/v/org.jetbrains.kotlinx/kotlinx-benchmark-runtime.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.jetbrains.kotlinx%22%20AND%20a:%22kotlinx-benchmark-runtime%22)
[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v?label=Gradle%20Plugin&metadataUrl=https://plugins.gradle.org/m2/org/jetbrains/kotlinx/kotlinx-benchmark-plugin/maven-metadata.xml)](https://plugins.gradle.org/plugin/org.jetbrains.kotlinx.benchmark)
[![IR](https://img.shields.io/badge/Kotlin%2FJS-IR%20supported-yellow)](https://kotl.in/jsirsupported)

kotlinx.benchmark is a toolkit for running benchmarks for multiplatform code written in Kotlin and running on the following supported targets: JVM, JavaScript and Native.

## Features

- Low noise and reliable results
- Statistical analysis
- Detailed performance reports

## Table of contents

<!--- TOC -->

- [Using in Your Projects](#using-in-your-projects)
  - [Gradle Setup](#gradle-setup)
    - [Kotlin DSL](#kotlin-dsl)
    - [Groovy DSL](#groovy-dsl)
  - [Target-specific configurations](#target-specific-configurations)
    - [Kotlin/JVM](#kotlinjvm)
    - [Kotlin/JS](#kotlinjs)
    - [Multiplatform](#multiplatform)
  - [Benchmark Configuration](#benchmark-configuration)
- [Examples](#examples)
- [Contributing](#contributing)

<!--- END -->

- **Additional links**
  - [Code Benchmarking: A Brief Overview](docs/benchmarking-overview.md)
  - [Understanding Benchmark Runtime](docs/benchmark-runtime.md)
  - [Configuring kotlinx-benchmark](docs/configuration-options.md)
  - [Interpreting and Analyzing Results](docs/interpreting-results.md)
  - [Creating Separate Source Sets](docs/seperate-source-sets.md)
  - [Tasks Overview](docs/tasks-overview.md)
  - [Compatibility Guide](docs/compatibility.md)
  - [Submitting issues and PRs](CONTRIBUTING.md)

## Using in Your Projects

The `kotlinx-benchmark` library is designed to work with Kotlin/JVM, Kotlin/JS, and Kotlin/Native targets. To get started, ensure you're using Kotlin 1.8.20 or newer and Gradle 8.0 or newer.

### Gradle Setup

<details open>
<summary>Kotlin DSL</summary>

1.  **Applying Benchmark Plugin**: Apply the benchmark plugin.

    ```kotlin
    plugins {
        id("org.jetbrains.kotlinx.benchmark") version "0.4.9"
    }
    ```

2.  **Specifying Repository**: Ensure you have `mavenCentral()` for dependencies lookup in the list of repositories:

    ```kotlin
    repositories {
        mavenCentral()
    }
    ```

    </details>

<details>
<summary>Groovy DSL</summary>

1.  **Applying Benchmark Plugin**: Apply the benchmark plugin.

    ```groovy
    plugins {
        id 'org.jetbrains.kotlin.plugin.allopen' version "1.8.21"
        id 'org.jetbrains.kotlinx.benchmark' version '0.4.9'
    }
    ```

2.  **Specifying Repository**: Ensure you have `mavenCentral()` in the list of repositories:

    ```groovy
    repositories {
        mavenCentral()
    }
    ```

    </details>

### Target-specific configurations

For different platforms, there may be distinct requirements and settings that need to be configured.

#### Kotlin/JVM

When benchmarking Kotlin/JVM code with Java Microbenchmark Harness (JMH), you should use the [allopen plugin](https://kotlinlang.org/docs/all-open-plugin.html). This plugin ensures your benchmark classes and methods are `open`, meeting JMH's requirements. Make sure to apply the jvm plugin. 

```kotlin
plugins {
    kotlin("jvm") version "1.8.21"
    kotlin("plugin.allopen") version "1.8.21"
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}
```

<details>
  <summary><b>Illustrative Example</b></summary>

Consider you annotated each of your benchmark classes with `@State(Scope.Benchmark)`:

```kotlin
@State(Scope.Benchmark)
class MyBenchmark {
    // Benchmarking-related methods and variables
    fun benchmarkMethod() {
        // benchmarking logic
    }
}
```

In Kotlin, classes and methods are `final` by default, which means they can't be overridden. This is incompatible with the operation of the Java Microbenchmark Harness (JMH), which needs to generate subclasses for benchmarking.

This is where the `allopen` plugin comes into play. With the plugin applied, any class annotated with `@State` is treated as `open`, which allows JMH to work as intended. Here's the Kotlin DSL configuration for the `allopen` plugin:

```kotlin
plugins {
    kotlin("plugin.allopen") version "1.8.21"
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}
```

This configuration ensures that your `MyBenchmark` class and its `benchmarkMethod` function are treated as `open`, allowing JMH to generate subclasses and conduct the benchmark.

</details>

You can alternatively mark your benchmark classes and methods `open` manually, but using the `allopen` plugin enhances code maintainability. For a practical example, please refer to [examples](examples/kotlin-kts).

#### Kotlin/JS

Specify a compiler like the [IR compiler](https://kotlinlang.org/docs/js-ir-compiler.html) and set benchmarking targets in one step. Here, `jsIr` and `jsIrBuiltIn` are both using the IR compiler. The former uses benchmark.js, while the latter uses Kotlin's built-in plugin.

```kotlin
kotlin {
    js('jsIr', IR) { 
        nodejs() 
    }
    js('jsIrBuiltIn', IR) { 
        nodejs() 
    }
}
```

#### Multiplatform

For multiplatform projects, add the `kotlinx-benchmark-runtime` dependency to the `commonMain` source set:

```kotlin
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.8")
            }
        }
    }
}
```

This setup enables running benchmarks in the main compilation of any registered targets. Another option is to register the compilation you want to run benchmarks from. The platform-specific artifacts will be resolved automatically. For a practical example, please refer to [examples](examples/multiplatform).

### Benchmark Configuration

In a `build.gradle` file create `benchmark` section, and inside it add a `targets` section.
In this section register all targets you want to run benchmarks from. 
Example for multiplatform project:

```kotlin
benchmark {
    targets {
        register("jvm")
        register("js")
        register("native")
        // Add this line if you are working with WebAssembly (experimental)
        // register("wasm")
    }
}
```

To further customize your benchmarks, add a `configurations` section within the `benchmark` block. By default, a `main` configuration is generated, but additional configurations can be added as needed:

```kotlin
benchmark {
    configurations {
        main {
            warmups = 20
            iterations = 10
            iterationTime = 3
        }
        smoke {
            warmups = 5
            iterations = 3
            iterationTime = 500
            iterationTimeUnit = "ms"
        }
        register("native")
        register("wasm") // Experimental
    }
}
```  
  
# Separate source sets for benchmarks

Often you want to have benchmarks in the same project, but separated from main code, much like tests. Here is how:

Define source set:
```groovy
sourceSets {
    benchmarks
}
```

Propagate dependencies and output from `main` sourceSet. 

```groovy
dependencies {
    benchmarksCompile sourceSets.main.output + sourceSets.main.runtimeClasspath 
}
```

You can also add output and compileClasspath from `sourceSets.test` in the same way if you want 
to reuse some of the test infrastructure.


Register `benchmarks` source set:

```groovy
benchmark {
    targets {
        register("benchmarks")    
    }
}
```

For a Kotlin Multiplatform project:

Define a new compilation in whichever target you'd like (e.g. `jvm`, `js`, etc):
```groovy
kotlin {
    jvm {
        compilations.create('benchmark') { associateWith(compilations.main) }
    }
}
```

Register it by its source set name (`jvmBenchmark` is the name for the `benchmark` compilation for `jvm` target):

```groovy
benchmark {
    targets {
        register("jvmBenchmark")    
    }
}
```

For comprehensive guidance on configuring your benchmark setup, please refer to our detailed documentation on [Configuring kotlinx-benchmark](docs/configuration-options.md).

# Examples

To help you better understand how to use the kotlinx-benchmark library, we've provided an [examples](examples) subproject. These examples showcase various use cases and offer practical insights into the library's functionality.

## Contributing

We welcome contributions to kotlinx-benchmark! If you want to contribute, please refer to our Contribution Guidelines.

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
    - [Kotlin/JS](#kotlinjs)
    - [Multiplatform](#multiplatform)
  - [Benchmark Configuration](#benchmark-configuration)
- [Examples](#examples)
- [Contributing](#contributing)

<!--- END -->

- **Additional links**
  - [Harnessing Code Performance: The Art and Science of Benchmarking](docs/benchmarking-overview.md)
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

1.  **Adding Dependency**: Add the `kotlinx-benchmark-runtime` dependency in your `build.gradle.kts` file.

    ```kotlin
    dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.8")
    }
    ```

2.  **Applying Benchmark Plugin**: Next, apply the benchmark plugin.

    ```kotlin
    plugins {
        kotlin("plugin.allopen") version "1.8.21"
        id("org.jetbrains.kotlinx.benchmark") version "0.4.8"
    }
    ```

3.  **Enabling AllOpen Plugin**: If your benchmark classes are annotated with `@State(Scope.Benchmark)`, apply the `allopen` plugin and specify the `State` annotation.

    ```kotlin
    plugins {
        kotlin("plugin.allopen") version "1.8.21"
    }
    ```

4.  **Using AllOpen Plugin**: The `allopen` plugin is used to satisfy JMH requirements, or all benchmark classes and methods should be `open`.

    ```
    allOpen {
        annotation("org.openjdk.jmh.annotations.State")
    }
    ```

5.  **Specifying Repository**: Ensure you have `mavenCentral()` for dependencies lookup in the list of repositories:

        ```kotlin
        repositories {
            mavenCentral()
        }
        ```

    </details>

<details>
<summary>Groovy DSL</summary>

1.  **Adding Dependency**: In your `build.gradle` file, include the `kotlinx-benchmark-runtime` dependency.

    ```groovy
    dependencies {
        implementation 'org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.8'
    }
    ```

2.  **Applying Benchmark Plugin**: Next, apply the benchmark plugin.

    ```groovy
    plugins {
        id 'org.jetbrains.kotlin.plugin.allopen' version "1.8.21"
        id 'org.jetbrains.kotlinx.benchmark' version '0.4.8'
    }
    ```

3.  **Enabling AllOpen Plugin**: If your benchmark classes are annotated with `@State(Scope.Benchmark)`, apply the `allopen` plugin and specify the `State` annotation.

    ```groovy
    plugins {
        id 'org.jetbrains.kotlin.plugin.allopen'
    }
    ```

4.  **Using AllOpen Plugin**: The `allopen` plugin is used to satisfy JMH requirements, or all benchmark classes and methods should be `open`.

    ```
    allOpen {
        annotation("org.openjdk.jmh.annotations.State")
    }
    ```

5.  **Specifying Repository**: Ensure you have `mavenCentral()` in the list of repositories:

        ```groovy
        repositories {
            mavenCentral()
        }
        ```

    </details>

### Target-specific configurations

#### Kotlin/JS

For Kotlin/JS, include the `nodejs()` method call in the `kotlin` block:

```kotlin
kotlin {
    js {
        nodejs()
    }
}
```

For Kotlin/JS, both Legacy and IR backends are supported. However, simultaneous target declarations such as `kotlin.js.compiler=both` or `js(BOTH)` are not feasible. Ensure each backend is separately declared. For a detailed configuration example, please refer to the [build script of the kotlin-multiplatform example project](https://github.com/Kotlin/kotlinx-benchmark/blob/master/examples/kotlin-multiplatform/build.gradle).

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

The platform-specific artifacts will be resolved automatically.

### Benchmark Configuration

Define your benchmark targets within the `benchmark` section in your `build.gradle` file:

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
    }
}
```

# Examples

The project contains [examples](https://github.com/Kotlin/kotlinx-benchmark/tree/master/examples) subproject that demonstrates using the library.

## Contributing

We welcome contributions to kotlinx-benchmark! If you want to contribute, please refer to our Contribution Guidelines.

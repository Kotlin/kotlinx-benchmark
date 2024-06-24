# Setting Up Kotlin/JVM and Java Projects for Benchmarking

In this guide, we'll take you through the steps to set up a Kotlin/JVM or Java project
for benchmarking using `kotlinx-benchmark`.

## Step-by-step Setup Guide

To configure Kotlin/JVM and Java projects for benchmarking, follow these steps:

1. Apply the benchmark plugin:

    <details open><summary>Kotlin DSL</summary>

    ```kotlin
    // build.gradle.kts
    plugins {
        id("org.jetbrains.kotlinx.benchmark") version "0.4.11"
    }
    ```

    </details>

    <details><summary>Groovy DSL</summary>

    ```groovy
    // build.gradle
    plugins {
        id 'org.jetbrains.kotlinx.benchmark' version '0.4.11'
    }
    ```

    </details>

2. Make sure to include the Gradle Plugin Portal for plugin lookup in the list of repositories:
 
    <details open><summary>Kotlin DSL</summary>

    ```kotlin
    // settings.gradle.kts
    pluginManagement {
        repositories {
            gradlePluginPortal()
        }
    }
    ```

    </details>

    <details><summary>Groovy DSL</summary>

    ```groovy
    // settings.gradle
    pluginManagement {
        repositories {
            gradlePluginPortal()
        }
    }
    ```

    </details>

3.  Next, add the `kotlinx-benchmark-runtime` dependency to the project:

    <details open><summary>Kotlin DSL</summary>

    ```kotlin
    // build.gradle.kts
    dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.11")
    }
    ```

    </details>

    <details><summary>Groovy DSL</summary>

    ```groovy
    // build.gradle
    dependencies {
        implementation 'org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.11'
    }
    ```

    </details>

4.  Include Maven Central in the list of repositories for dependency lookup:

    <details open><summary>Kotlin DSL</summary>

    ```kotlin
    // build.gradle.kts
    repositories {
        mavenCentral()
    }
    ```

    </details>

    <details><summary>Groovy DSL</summary>

    ```groovy
    // build.gradle
    repositories {
        mavenCentral()
    }
    ```

    </details>

5. Apply [allopen plugin](https://kotlinlang.org/docs/all-open-plugin.html) if you have benchmark classes in Kotlin:

    <details open><summary>Kotlin DSL</summary>

    ```kotlin
    // build.gradle.kts
    plugins {
        kotlin("plugin.allopen") version "1.9.20"
    }

    allOpen {
        annotation("org.openjdk.jmh.annotations.State")
    }
    ```

    </details>

    <details><summary>Groovy DSL</summary>

    ```groovy
    // build.gradle
    plugins {
        id 'org.jetbrains.kotlin.plugin.allopen' version "1.9.20"
    }
    
    allOpen {
        annotation("org.openjdk.jmh.annotations.State")
    }
    ```

    </details>

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
        kotlin("plugin.allopen") version "1.9.20"
    }

    allOpen {
        annotation("org.openjdk.jmh.annotations.State")
    }
    ```

    This configuration ensures that your `MyBenchmark` class and its `benchmarkMethod` function are treated as `open`.

    </details>

    You can alternatively mark your benchmark classes and methods `open` manually, but using the `allopen` plugin enhances code maintainability.

6. Designate the `main` source set as a benchmark target:

    <details open><summary>Kotlin DSL</summary>

    ```kotlin
    // build.gradle.kts
    benchmark {
        targets {
            register("main")
        }
    }
    ```

    </details>

    <details><summary>Groovy DSL</summary>

    ```kotlin
    // build.gradle
    benchmark {
        targets {
            register("main")
        }
    }
    ```

    </details>

   This informs the `kotlinx-benchmark` tool that benchmarks reside within `main` source set.

## Writing Benchmarks

After completing the setup of your project, you're ready to dive into writing benchmarks.
`kotlinx-benchmark` leverages the Java Microbenchmark Harness (JMH) toolkit to execute benchmarks on the JVM.
As a result, it automatically includes the necessary dependency on JMH, allowing you to harness its API for crafting benchmarks:

```kotlin
import org.openjdk.jmh.annotations.*
import java.util.concurrent.*
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.random.Random

@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 10)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
class MyBenchmark {
    private var data = 0.0

    @Setup
    fun setUp() {
        data = Random.nextDouble()
    }

    @Benchmark
    fun sqrtBenchmark(): Double {
        return sqrt(data)
    }

    @Benchmark
    fun cosBenchmark(): Double {
        return cos(data)
    }
}
```

See [writing benchmarks](writing-benchmarks.md) for a complete guide for writing benchmarks.

### Running Benchmarks

To run your benchmarks, run `benchmark` Gradle task in the project.
In the terminal, navigate to the project's root directory and run `./gradlew benchmark`.

For more details about the tasks created by the `kotlinx-benchmark` plugin, refer to [this guide](tasks-overview.md).

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

Refer to our [comprehensive guide](configuration-options.md) to learn about configuration options and how they affect benchmark execution.

### Separate source set for benchmarks

Often you want to have benchmarks in the same project, but separated from main code, much like tests. 
Refer to our [detailed documentation](separate-benchmark-source-set.md) on configuring your project to set up a separate source set for benchmarks.

## Examples

Explore sample [Kotlin/JVM](/examples/kotlin) and [Java](/examples/java) benchmarking projects that use `kotlinx-benchmark`.
These examples showcase how to structure benchmarking projects using `kotlinx-benchmark`.

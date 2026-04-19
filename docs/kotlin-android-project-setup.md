# Setting Up Android Benchmarks for Kotlin Multiplatform

In this guide, we'll take you through the steps to set up an Android module in 
Kotlin Multiplatform for benchmarking using `kotlinx-benchmark`.

## Step-by-step Setup Guide

1. Check that the correct multiplatform module is being used.

    Make sure that you are applying the `com.android.kotlin.multiplatform.library`
    plugin, and not `com.android.library` or `com.android.application`. The 
    latter are being [deprecated](https://developer.android.com/kotlin/multiplatform/plugin) by Google and are not supported by 
    `kotlinx-benchmark`. 

    Then apply the benchmark plugin

    ```kotlin
    // build.gradle.kts
    plugins {
        kotlin("multiplatform") version "2.3.20"
        id("com.android.kotlin.multiplatform.library") version "9.1.1"
        id("org.jetbrains.kotlinx.benchmark") version "0.4.16"
    }
   ```
   
2. Make sure to include the Google and Gradle Plugin Portal for plugin lookup in
   the list of repositories:

    ```kotlin
    // settings.gradle.kts
    pluginManagement {
        repositories {
            gradlePluginPortal()
            google()
        }
    }
    ```

3.  Next, add the `kotlinx-benchmark-runtime` dependency to the project:

    ```kotlin
    // build.gradle.kts
    kotlin {
        sourceSets {
            commonMain {
                dependencies {
                    implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.16")
                }
            }
        }
    }
    ```

4.  Include Maven Central and Google in the list of repositories for dependency lookup:

    ```kotlin
    // build.gradle.kts
    repositories {
        mavenCentral()
        google()
    }
    ```

5. Register the `android` benchmark target:

    ```kotlin
    benchmarks {
        targets {
            register("android") {
                this as AndroidBenchmarkTarget
                 // Required unless the `ANDROID_HOME` environment variable is set
                sdkDir = "/path/to/android/sdk"
            }
        }    
   }
    ```
   
6. Configure the Jetpack Microbenchmark run parameters:

    For experimentation and testing, it is recommended to have the following
    settings enabled:
    
    a. Disabled profiling
    b. Allow running on emulators
    c. `dry-run` enabled. This will only use a single iteration pr. test.

    ```
       register("android") {
           this as AndroidBenchmarkTarget
           profilingMode = ProfilingMode.None
           instrumentationRunnerArguments.putAll(mapOf(
               "androidx.benchmark.suppressErrors" to "EMULATOR",
               "androidx.benchmark.dryRunMode.enable" to "true
           ))
       }
    ```
   
    For actionable and consistent results, benchmarks should be run on a proper
    device instead of on an emulator. See the Jetpack Microbenchmark 
    [guide](https://developer.android.com/topic/performance/benchmarking/microbenchmark-overview#benchmark-consistency) for more details.

## Writing Benchmarks

After completing the setup of your project, you're ready to dive into writing 
benchmarks for Android. Benchmark files can either be placed in the `common` or 
`androidMain` source sets.

Benchmarks can be written using the standard `kotlinx-benchmark` annotations, 
but due to restrictions in Jetpack Multiplatform, only a subset are currently
supported.

The following is being ignored:

- `@BenchmarkMode(...)`
- `@OutputTimeUnit(...)`
- `@Warmup(...)`
- `@Measurement(...)`

```kotlin
// Example benchmark
import kotlinx.benchmark.*
import kotlin.math.*

@State(Scope.Benchmark)
class AndroidTestBenchmark {
    private var data = 0.0

    @Setup
    fun setUp() {
        data = 3.0
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

## Running Benchmarks

Running benchmarks require an attached device that is visible to `ADB`:

```shell
> adb devices
List of devices attached
emulator-5554	device
```

To run your benchmarks, run the `androidBenchmark` Gradle task in the project.

```shell
> ./gradlew androidBenchmark
```

It is possible to restrict benchmarks to a specific device, using:

```shell
> ANDROID_SERIAL=emulator-5554 ./gradlew androidBenchmark
```

Once benchmarks have run, `kotlinx-benchmark` will automatically pull results
from all connected devices and copy them to the `/build/reports/benchmarks/android` 
directory.

Two main result files exist:
    
 - `/build/reports/benchmarks/android/<deviceId>.txt`: Contains a simple 
   summary of all benchmarks in a similar format created by Jetpack 
   Microbenchmark
- `/build/reports/benchmarks/android/<deviceId>.json`: Contains all the 
   underlying benchark measurement data. This format is Android-specific and 
   _not_ compatible with JMH.

## Benchmark Configuration Profiles

`kotlinx-benchmark` makes it possible to configure [different benchmark profiles](./configuration-options.md)
with a default `main` being generated by the plugin.

Due to restrictions in the Android Gradle Plugin, it is not possible to run
Android benchmarks on any other profile than `main`.

This means that if you have the following configurations:

```kotlin
benchmark {
    configurations {
        register("main") { ... }
        register("smoke") { ... }
    }
}
```

Only one benchmark is available:

```shell
./gradlew androidBenchmark # Available
./gradlew androidSmokeBenchmark # Not available
```
Note, `BenchmarkConfiguration.reportFormat` is currently being ignored when
producing benchmark reports, also for the `main` configuration.

## Separate source set for benchmarks

Often you want to have benchmarks in the same project but separated from the 
main code, much like tests. 

Due to restrictions in the Android Gradle Plugin, it is not possible to split
them using custom compilations as described in [our documentation](separate-benchmark-source-set.md).

Instead, a similar result can be achieved by creating adding a new 
`androidBenchmark` directory and configure it as a source set:

```kotlin
kotlin {
    sourceSets {
        androidMain {
            kotlin.srcDirs(
                "src/androidMain/kotlin",
                "src/androidBenchmark/kotlin"
            )
        }
    }
}
```

Note that this method will only separate the tests in the project view, not
in the final build artifact being produced.

## Examples

Explore the [Kotlin Multiplatform](/examples/kotlin-multiplatform) sample projects that use `kotlinx-benchmark`.
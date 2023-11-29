## Overview of Tasks Provided by kotlinx-benchmark Gradle Plugin

The kotlinx-benchmark plugin creates different Gradle tasks depending on how it is configured.
For each pair of configuration profile and registered target a task is created to execute that profile on the respective platform.
To learn more about configuration profiles, refer to [configuration-options.md](configuration-options.md).

### Example Configuration

To illustrate, consider the following `kotlinx-benchmark` configuration:

```kotlin
// build.gradle.kts
benchmark {
    configurations {
        named("main") {
            iterations = 20
            warmups = 20
            iterationTime = 1
            iterationTimeUnit = "s"
        }
        register("smoke") {
            include("Essential")
            iterations = 10
            warmups = 10
            iterationTime = 200
            iterationTimeUnit = "ms"
        }
    }

    targets {
        register("jvm")
        register("js")
    }
}
```

## Tasks for the "main" Configuration Profile

- **`benchmark`**:
    - Runs benchmarks within the "main" profile for all registered targets. 
    - In our example, `benchmark` runs benchmarks within the "main" profile in both `jvm` and `js` targets.

- **`<targetName>Benchmark`**:
    - Runs benchmarks within the "main" profile for a particular target.
    - In our example, `jvmBenchmark` runs benchmarks within the "main" profile in the `jvm` target, while `jsBenchmark` runs them in the `js` target.

## Tasks for Custom Configuration Profiles

- **`<configName>Benchmark`**:
    - Runs benchmarks within `<configName>` profile in all registered targets.
    - In our example, `smokeBenchmark` runs benchmarks within the "smoke" profile.

- **`<targetName><configName>Benchmark`**:
    - Runs benchmarks within `<configName>` profile in `<targetName>` target.
    - In our example, `jvmSmokeBenchmark` runs benchmarks within the "smoke" profile in `jvm` target while `jsSmokeBenchmark` runs them in `js` target.

## Other useful tasks

- **`<targetName>BenchmarkJar`**:
    - Created only when a Kotlin/JVM target is registered for benchmarking.
    - Produces a self-contained executable JAR file in `build/benchmarks/<targetName>/jars/` directory of your project that contains your benchmarks in `<targetName>` target, and all essential JMH infrastructure code.
    - The JAR file can be run using `java -jar path-to-the.jar` command with relevant options. Run with `-h` to see the available options.
    - The JAR file can be used for running JMH profilers.
    - In our example, `jvmBenchmarkJar` produces a JAR file in `build/benchmarks/jvm/jars/` directory that contains benchmarks in `jvm` target.

# Overview of Tasks provided by kotlinx-benchmark Gradle Plugin

The kotlinx-benchmark plugin creates different Gradle tasks depending on how it is configured.
For each pair of configuration profile and registered target it creates a task to run that profile on the platform.
To learn more about configuration profiles, refer to [configuration-options.md](configuration-options.md).

For illustration purposes consider that the kotlinx-benchmark plugin is configured in the following way:

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

## Running benchmarks within the "main" configuration profile

### `benchmark`

This task runs benchmarks within the "main" profile in all registered targets.

### `<targetName>Benchmark`

This task runs benchmarks within the "main" profile for a particular target. In our example, `jvmBenchmark` runs benchmarks within the "main" profile in `jvm` target, 
while `jsBenchmark` runs them in `js` target.

## Running benchmarks within a created configuration profile

### `<configName>Benchmark`

This task runs benchmarks within `<configName>` profile in all registered targets. In our example, `smokeBenchmark` runs benchmarks within the "smoke" profile.


### `<targetName><configName>Benchmark`

This task runs benchmarks within `<configName>` profile in `<targetName>` target. In our example, `jvmSmokeBenchmark` runs benchmarks within the "smoke" profile in `jvm` target,
while `jsSmokeBenchmark` runs them in `js` target.

## Other useful tasks

### `<targetName>BenchmarkJar`

This task is created only when a Kotlin/JVM target is registered for benchmarking.
It produces a self-contained executable JAR in `build/benchmarks/<targetName>/jars/` directory of your project 
that contains your benchmarks in `<targetName>` target, and all essential JMH infrastructure code.
The JAR file can be run using `java -jar path-to-the.jar` command with relevant options. Run with `-h` to see the available options.
In our example, `jvmBenchmarkJar` produces a JAR file in `build/benchmarks/jvm/jars/` directory that contains benchmarks in `jvm` target.

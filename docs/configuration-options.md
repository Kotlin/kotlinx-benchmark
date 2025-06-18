# Mastering kotlinx-benchmark Configuration

This is a comprehensive guide to configuration options that help fine-tune your benchmarking setup to suit your specific needs.

## The `configurations` Section

The `configurations` section of the `benchmark` block serves as the control center for setting the parameters of your benchmark profiles. The library provides a default configuration profile named "main", which can be configured according to your needs just like any other profile. Here's a basic structure of how configurations can be set up:

```kotlin
// build.gradle.kts
benchmark {
    configurations {
        register("smoke") {
            // Configure this configuration profile here
        }
        // here you can create additional profiles
    }
}
```

## Understanding Configuration Profiles

Configuration profiles dictate the execution pattern of benchmarks:

- Utilize `include` and `exclude` options to select specific benchmarks for a profile. By default, every benchmark is included.
- Each configuration profile translates to a task in the `kotlinx-benchmark` Gradle plugin. For instance, the task `smokeBenchmark` is tailored to run benchmarks based on the `"smoke"` configuration profile. For an overview of tasks, refer to [tasks-overview.md](tasks-overview.md).

## Core Configuration Options

Note that values defined in the build script take precedence over those specified by annotations in the code.

| Option                              | Description                                                                                                                                             | Possible Values                                            | Corresponding Annotation                              |
|-------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------|-------------------------------------------------------|
| `iterations`                        | Sets the number of iterations for measurements.                                                                                                         | Positive Integer                                           | `@Measurement(iterations: Int, ...)`                  |
| `warmups`                           | Sets the number of iterations for system warming, ensuring accurate measurements.                                                                       | Non-negative Integer                                       | `@Warmup(iterations: Int)`                            |
| `iterationTime`                     | Sets the duration for each iteration, both measurement and warm-up.                                                                                     | Positive Integer                                           | `@Measurement(..., time: Int, ...)`                   |
| `iterationTimeUnit`                 | Defines the unit for `iterationTime`.                                                                                                                   | Time unit, see below                                       | `@Measurement(..., timeUnit: BenchmarkTimeUnit, ...)` |
| `outputTimeUnit`                    | Sets the unit for the results display.                                                                                                                  | Time unit, see below                                       | `@OutputTimeUnit(value: BenchmarkTimeUnit)`           |
| `mode`                              | Selects "thrpt" (Throughput) for measuring the number of function calls per unit time or "avgt" (AverageTime) for measuring the time per function call. | `"thrpt"`, `"Throughput"`, `"avgt"`, `"AverageTime"`       | `@BenchmarkMode(value: Mode)`                         |
| `include("…")`                      | Applies a regular expression to include benchmarks that match the substring in their fully qualified names.                                             | Regex pattern                                              | -                                                     |
| `exclude("…")`                      | Applies a regular expression to exclude benchmarks that match the substring in their fully qualified names.                                             | Regex pattern                                              | -                                                     |
| `param("name", "value1", "value2")` | Assigns values to a public mutable property with the specified name, annotated with `@Param`.                                                           | String values that represent valid values for the property | `@Param`                                              |
| `reportFormat`                      | Defines the benchmark report's format options.                                                                                                          | `"json"`(default), `"csv"`, `"scsv"`, `"text"`             | -                                                     |

The following values can be used for specifying time unit:
- "NANOSECONDS", "ns", "nanos"
- "MICROSECONDS", "us", "micros"
- "MILLISECONDS", "ms", "millis"
- "SECONDS", "s", "sec"
- "MINUTES", "m", "min"

## Platform-Specific Configuration Options

The options listed in the following sections allow you to tailor the benchmark execution behavior for specific platforms:

### Kotlin/Native
| Option                                        | Description                                                                                                            | Possible Values                    | Default Value    |
|-----------------------------------------------|------------------------------------------------------------------------------------------------------------------------|------------------------------------|------------------|
| `advanced("nativeFork", "value")`             | Executes iterations within the same process ("perBenchmark") or each iteration in a separate process ("perIteration"). | `"perBenchmark"`, `"perIteration"` | `"perBenchmark"` |
| `advanced("nativeGCAfterIteration", value)`   | Whether to trigger garbage collection after each iteration.                                                            | `true`, `false`                    | `false`          |

By default, to run benchmarks, the library uses a release type of native binary, which is optimized one and without debug information.
It is possibly to change the type to debug by setting it during benchmark targets configuration:

```Kotlin
benchmark {
    targets {
        register("native") {
            this as NativeBenchmarkTarget
            buildType = NativeBuildType.DEBUG
        }
    }
}
```

### Kotlin/JVM
| Option                                      | Description                                                | Possible Values                        | Default Value  |
|---------------------------------------------|------------------------------------------------------------|----------------------------------------|----------------|
| `advanced("jvmForks", value)`               | Specifies the number of times the harness should fork.     | Non-negative Integer, `"definedByJmh"` | `1`            |

**Notes on "jvmForks":**
- **0** - "no fork", i.e., no subprocesses are forked to run benchmarks.
- A positive integer value – the amount used for all benchmarks in this configuration.
- **"definedByJmh"** – Let JMH determine the amount, using the value in the [`@Fork` annotation](https://javadoc.io/doc/org.openjdk.jmh/jmh-core/latest/org/openjdk/jmh/annotations/Fork.html) for the benchmark function or its enclosing class. If not specified by `@Fork`, it defaults to [Defaults.MEASUREMENT_FORKS (`5`)](https://javadoc.io/doc/org.openjdk.jmh/jmh-core/latest/org/openjdk/jmh/runner/Defaults.html#MEASUREMENT_FORKS).

The library offers the flexibility to specify the version of the Java Microbenchmark Harness (JMH) to use when running benchmarks on the JVM.
The default version is set to `1.37`, but you can customize it while registering a JVM target for benchmarking:

```kotlin
benchmark {
    targets {
        register("jvmBenchmarks") {
            this as JvmBenchmarkTarget
            jmhVersion = "1.38"
        }
    }
}
```

Alternatively, you can utilize the project property `benchmarks_jmh_version` to achieve the same effect.

> [!WARNING]
> While it is possible to register multiple JVM benchmark targets with different JMH versions,
> such configurations are not supported. Using such configurations may result in runtime errors.

> [!NOTE]
> It is recommended to change JMH version only when a new JMH version was released,
> but `kotlinx-benchmark` plugin applied to a project is still using an older version.

### Kotlin/JS & Kotlin/Wasm
| Option                                        | Description                                                                                           | Possible Values | Default Value |
|-----------------------------------------------|-------------------------------------------------------------------------------------------------------|-----------------|---------------|
| `advanced("jsUseBridge", value)`              | Generate special benchmark bridges to stop inlining optimizations.                                    | `true`, `false` | `true`        |

**Note:** In the Kotlin/JS target, the "jsUseBridge" option only takes effect when the `BuiltIn` benchmark executor is selected.

By default, kotlinx-benchmark employs the `benchmark.js` library for running benchmarks in Kotlin/JS. 
However, users have the option to select the library's built-in benchmarking implementation:

```kotlin
benchmark {
    targets {
        register("jsBenchmarks") {
            this as JsBenchmarkTarget
            jsBenchmarksExecutor = JsBenchmarksExecutor.BuiltIn
        }
    }
}
```

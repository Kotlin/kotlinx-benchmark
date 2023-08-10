# Mastering kotlinx-benchmark Configuration

This is a comprehensive guide to configuration options that help fine-tune your benchmarking setup to suit your specific needs.
The `configurations` section of the `benchmark` block serves as the control center for setting the parameters of your benchmark profiles.
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
Configuration profiles enable you to execute benchmarks in specific ways.
`include` and `exclude` options are used to specify the benchmarks to run in this profile.
By default, all benchmarks are included.
`kotlinx-benchmark` Gradle plugin creates tasks for running benchmarks in each configuration profile. For example, 
task `smokeBenchmark` will run benchmarks according to the `"smoke"` configuration profile. See [tasks-overview.md](tasks-overview.md).
Use the options described below to configure how the benchmarks should be run.

## Core Configuration Options

Be aware that values defined in the build script will override those specified by annotations in the code.

| Option                              | Description                                                                                                                  | Possible Values               | Corresponding Annotation                            |
| ----------------------------------- |------------------------------------------------------------------------------------------------------------------------------|-------------------------------|-----------------------------------------------------|
| `iterations`                        | Sets the number of iterations for measurements.                                                                              | Integer                       | @Measurement(iterations: Int, ...)                  |
| `warmups`                           | Sets the number of iterations for system warming, ensuring accurate measurements.                                            | Integer                       | @Warmup(iterations: Int)                            |
| `iterationTime`                     | Sets the duration for each iteration, both measurement and warm-up.                                                        | Integer                       | @Measurement(..., time: Int, ...)                   |
| `iterationTimeUnit`                 | Defines the unit for `iterationTime`.                                                                                        | Time unit, see below          | @Measurement(..., timeUnit: BenchmarkTimeUnit, ...) |
| `outputTimeUnit`                    | Sets the unit for the results display.                                                                                       | Time unit, see below          | @OutputTimeUnit(value: BenchmarkTimeUnit)           |
| `mode`                              | Selects "thrpt" for measuring the number of function calls per unit time or "avgt" for measuring the time per function call. | "thrpt", "avgt"               | @BenchmarkMode                                      |
| `include("…")`                      | Applies a regular expression to include benchmarks that match the substring in their fully qualified names.                  | Regex pattern                 | -                                                   |
| `exclude("…")`                      | Applies a regular expression to exclude benchmarks that match the substring in their fully qualified names.                  | Regex pattern                 | -                                                   |
| `param("name", "value1", "value2")` | Assigns values to a public mutable property with the specified name, annotated with `@Param`.                                | Any string values             | @Param                                              |
| `reportFormat`                      | Defines the benchmark report's format options.                                                                               | "json", "csv", "scsv", "text" | -                                                   |

The following values can be used for specifying time unit:
- "NANOSECONDS", "ns", "nanos"
- "MICROSECONDS", "us", "micros"
- "MILLISECONDS", "ms", "millis"
- "SECONDS", "s", "sec"
- "MINUTES", "m", "min"

## Platform-Specific Configuration Options

The options below control benchmark execution in specific platforms:

### Kotlin/Native
| Option                                        | Description                                                                                                            | Possible Values                | Default Value  |
|-----------------------------------------------|------------------------------------------------------------------------------------------------------------------------|--------------------------------|----------------|
| `advanced("nativeFork", "value")`             | Executes iterations within the same process ("perBenchmark") or each iteration in a separate process ("perIteration"). | "perBenchmark", "perIteration" | "perBenchmark" |
| `advanced("nativeGCAfterIteration", value)`   | Whether to trigger garbage collection after each iteration.                                                            | `true`, `false`                | `false`        |

### Kotlin/JVM
| Option                                      | Description                                                | Possible Values                | Default Value  |
|---------------------------------------------|------------------------------------------------------------|--------------------------------|----------------|
| `advanced("jvmForks", value)`               | Specifies the number of times the harness should fork.     | Integer, "definedByJmh"        | `1`            |

**Notes on "jvmForks":**
- **0** - "no fork", i.e., no subprocesses are forked to run benchmarks.
- A positive integer value – the amount used for all benchmarks in this configuration.
- **"definedByJmh"** – Let JMH determine the amount, using the value in the [`@Fork` annotation](https://javadoc.io/static/org.openjdk.jmh/jmh-core/1.21/org/openjdk/jmh/annotations/Fork.html) for the benchmark function or its enclosing class. If not specified by `@Fork`, it defaults to [Defaults.MEASUREMENT_FORKS (`5`)](https://javadoc.io/static/org.openjdk.jmh/jmh-core/1.21/org/openjdk/jmh/runner/Defaults.html#MEASUREMENT_FORKS).

### Kotlin/JS & Kotlin/Wasm
| Option                                        | Description                                                                                           | Possible Values | Default Value |
|-----------------------------------------------|-------------------------------------------------------------------------------------------------------|-----------------|---------------|
| `advanced("jsUseBridge", value)`              | Generate special benchmark bridges to stop inlining optimizations.                                    | `true`, `false` | `true`        |

**Note:** "jsUseBridge" works only when the `BuiltIn` benchmark executor is selected.

With this guide at your side, you're ready to optimize your benchmarking process with `kotlinx-benchmark`. Happy benchmarking!
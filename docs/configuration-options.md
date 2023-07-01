# Mastering kotlinx-benchmark Configuration

Unleash the power of `kotlinx-benchmark` with our comprehensive guide, highlighting the breadth of configuration options that help fine-tune your benchmarking setup to suit your specific needs. Dive into the heart of the configuration process with both basic and advanced settings, offering a granular level of control to realize accurate, reliable performance measurements every time.

## Core Configuration Options: The Essential Settings

The `configurations` section of the `benchmark` block is where you control the parameters of your benchmark profiles. Each configuration offers a rich array of settings. Be aware that values defined in the build script will override those specified by annotations in the code.

| Option                              | Description                                                                                                                          | Possible Values                      | Corresponding Annotation |
| ----------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------ | ------------------------ |
| `iterations`                        | Sets the number of iterations for measurements.                                                                                 | Integer                              | @BenchmarkMode           |
| `warmups`                           | Sets the number of iterations for system warming, ensuring accurate measurements.                                               | Integer                              | @Warmup                  |
| `iterationTime`                     | Sets the duration for each iteration, both measurement and warm-up.                                                             | Integer                              | @Measurement             |
| `iterationTimeUnit`                 | Defines the unit for `iterationTime`.                                                                                              | "ns", "μs", "ms", "s", "m", "h", "d" | @Measurement             |
| `outputTimeUnit`                    | Sets the unit for the results display.                                                                                          | "ns", "μs", "ms", "s", "m", "h", "d" | @OutputTimeUnit          |
| `mode`                              | Selects "thrpt" for measuring the number of function calls per unit time or "avgt" for measuring the time per function call. | "thrpt", "avgt"                      | @BenchmarkMode           |
| `include("…")`                      | Applies a regular expression to include benchmarks that match the substring in their fully qualified names.                          | Regex pattern                        | -                        |
| `exclude("…")`                      | Applies a regular expression to exclude benchmarks that match the substring in their fully qualified names.                          | Regex pattern                        | -                        |
| `param("name", "value1", "value2")` | Assigns values to a public mutable property, annotated with `@Param`.                                                                | Any string values                    | @Param                   |
| `reportFormat`                      | Defines the benchmark report's format options.                                                                                       | "json", "csv", "scsv", "text"        | -                        |

## Expert Configuration Options: The Power Settings

The power of kotlinx-benchmark extends beyond basic settings. Delve into platform-specific options for tighter control over your benchmarks:

| Option                                        | Platform               | Description                                                                                                            | Possible Values                                  | Corresponding Annotation |
| --------------------------------------------- | ---------------------- | ---------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------ | ------------------------ |
| `advanced("nativeFork", "value")`             | Kotlin/Native          | Executes iterations within the same process ("perBenchmark") or each iteration in a separate process ("perIteration"). | "perBenchmark", "perIteration"                   | -                        |
| `advanced("nativeGCAfterIteration", "value")` | Kotlin/Native          | Triggers garbage collection after each iteration when set to `true`.                                                   | `true`, `false`                                  | -                        |
| `advanced("jvmForks", "value")`               | Kotlin/JVM             | Determines how many times the harness should fork.                                                                     | "0" (no fork), "1", "definedByJmh" (JMH decides) | @Fork                    |
| `advanced("jsUseBridge", "value")`            | Kotlin/JS, Kotlin/Wasm | Disables the generation of benchmark bridges to stop inlining optimizations when set to `false`.                       | `true`, `false`                                  | -                        |

With this guide at your side, you're ready to optimize your benchmarking process with `kotlinx-benchmark`. Happy benchmarking!
# Configuring kotlinx-benchmark

kotlinx-benchmark offers a plethora of configuration options that enable you to customize your benchmarking setup according to your precise needs. This advanced guide provides an in-depth explanation of how to setup your benchmark configurations, alongside detailed insights into kotlinx's functionalities.

## Table of Contents

- [Step 1: Laying the Foundation – Establish Benchmark Targets](#step-1)
- [Step 2: Tailoring the Setup – Create Benchmark Configurations](#step-2)
- [Step 3: Fine-tuning Your Setup – Understanding and Setting Configuration Options](#step-3)
  - [Basic Configuration Options: The Essential Settings](#step-3a)
  - [Advanced Configuration Options: The Power Settings](#step-3b)
- [The Benchmark Configuration in Action: An In-Depth Example](#example)

## Step 1: Laying the Foundation – Establish Benchmark Targets <a name="step-1"></a>

To start off, define the `benchmark` section within your `build.gradle` file. This section is your playground where you register the compilations you wish to run benchmarks on, within a `targets` subsection.

Targets can be registered in two ways. Either by their name, such as `"jvm"`, which registers its `main` compilation, meaning `register("jvm")` and `register("jvmMain")` will register the same compilation. Alternatively, you can register a source set, for instance, `"jvmTest"` or `"jsBenchmark"`, which will register the corresponding compilation. Here's an illustration using a multiplatform project:

```groovy
benchmark {
    targets {
        register("jvm")
        register("js")
        register("native")
        register("wasm") // Experimental
    }
}
```

For detailed guidance on creating separate source sets for benchmarks in your Kotlin project, please refer to [Benchmarking with Gradle: Creating Separate Source Sets](separate-source-sets.md).

## Step 2: Tailoring the Setup – Create Benchmark Configurations <a name="step-2"></a>

Having laid the groundwork with your targets, the next phase involves creating configurations for your benchmarks. You accomplish this by adding a `configurations` subsection within your `benchmark` block.

The kotlinx benchmark toolkit automatically creates a `main` configuration as a default. However, you can mold this tool to suit your needs by creating additional configurations. These configurations are your control knobs, letting you adjust the parameters of your benchmark profiles. Here's how:

```groovy
benchmark {
    configurations {
        main {
            // Configuration parameters for the default profile go here
        }
        smoke {
            // Create and configure a "smoke" configuration.
        }
    }
}
```

## Step 3: Fine-tuning Your Setup – Understanding and Setting Configuration Options <a name="step-3"></a>

Each configuration brings a bundle of options to the table, providing you with the flexibility to meet your specific benchmarking needs. We delve into these options to give you a better understanding and help you make the most of the basic and advanced settings:

**Note:** Many of these configuration options correspond to annotations in the benchmark code. Please be aware that any values provided in the build script will override those defined by annotations in the code.

### Basic Configuration Options: The Essential Settings <a name="step-3a"></a>

| Option                              | Description                                                                                                                          | Default Value | Possible Values                      | Corresponding Annotation |
| ----------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------ | ------------- | ------------------------------------ | ------------------------ |
| `iterations`                        | Specifies the number of iterations for measurements.                                                                                 | -             | Integer                              | @BenchmarkMode           |
| `warmups`                           | Specifies the number of iterations for system warming, ensuring accurate measurements.                                               | -             | Integer                              | @Warmup                  |
| `iterationTime`                     | Specifies the duration for each iteration, both measurement and warm-up.                                                             | -             | Integer                              | @Measurement             |
| `iterationTimeUnit`                 | Specifies the unit for `iterationTime`.                                                                                              | -             | "ns", "μs", "ms", "s", "m", "h", "d" | @Measurement             |
| `outputTimeUnit`                    | Specifies the unit for the results display.                                                                                          | -             | "ns", "μs", "ms", "s", "m", "h", "d" | @OutputTimeUnit          |
| `mode`                              | Selects between "thrpt" for measuring the number of function calls per unit time or "avgt" for measuring the time per function call. | -             | "thrpt", "avgt"                      | @BenchmarkMode           |
| `include("…")`                      | Applies a regular expression to include benchmarks that match the substring in their fully qualified names.                          | -             | Regex pattern                        | -                        |
| `exclude("…")`                      | Applies a regular expression to exclude benchmarks that match the substring in their fully qualified names.                          | -             | Regex pattern                        | -                        |
| `param("name", "value1", "value2")` | Assigns values to a public mutable property, annotated with `@Param`.                                                                | -             | Any string values                    | @Param                   |
| `reportFormat`                      | Defines the benchmark report's format options.                                                                                       | "json"        | "json", "csv", "scsv", "text"        | -                        |

### Advanced Configuration Options: The Power Settings <a name="step-3b"></a>

Beyond the basics, kotlinx allows you to take a deep dive into platform-specific settings, offering more control over your benchmarks:

| Option                                        | Platform               | Description                                                                                                            | Default Value  | Possible Values                                  | Corresponding Annotation |
| --------------------------------------------- | ---------------------- | ---------------------------------------------------------------------------------------------------------------------- | -------------- | ------------------------------------------------ | ------------------------ |
| `advanced("nativeFork", "value")`             | Kotlin/Native          | Executes iterations within the same process ("perBenchmark") or each iteration in a separate process ("perIteration"). | "perBenchmark" | "perBenchmark", "perIteration"                   | -                        |
| `advanced("nativeGCAfterIteration", "value")` | Kotlin/Native          | Triggers garbage collection after each iteration when set to `true`.                                                   | `false`        | `true`, `false`                                  | -                        |
| `advanced("jvmForks", "value")`               | Kotlin/JVM             | Determines how many times the harness should fork.                                                                     | "1"            | "0" (no fork), "1", "definedByJmh" (JMH decides) | @Fork                    |
| `advanced("jsUseBridge", "value")`            | Kotlin/JS, Kotlin/Wasm | Disables the generation of benchmark bridges to stop inlining optimizations when set to `false`.                       | -              | `true`, `false`                                  | -                        |

## The Benchmark Configuration in Action: An In-Depth Example <a name="example"></a>

```groovy
benchmark {
    configurations {
        main {
            warmups = 20 // Execute 20 iterations for system warming to stabilize the JVM and ensure accurate measurements
            iterations = 10 // Perform 10 iterations for the actual measurement
            iterationTime = 3 // Each iteration lasts for 3 seconds
            iterationTimeUnit = "s" // Time unit for iterationTime is seconds
            mode = "avgt" // Benchmarking mode is set to average time per function call
            outputTimeUnit = "ms" // The results will be displayed in milliseconds
            include(".*MyBenchmark.*") // Only include benchmarks that match this regular expression pattern
            param("size", "100", "200") // Assign two potential values ("100" and "200") to a property annotated with @Param
            reportFormat = "json" // The benchmark report will be generated in JSON format for easy parsing and visualization
        }
        smoke {
            warmups = 5 // Perform 5 warmup iterations
            iterations = 3 // Perform 3 measurement iterations
            iterationTime = 500 // Each iteration lasts for 500 milliseconds
            iterationTimeUnit = "ms" // Time unit for iterationTime is milliseconds
            advanced("nativeFork", "perIteration") // Execute each iteration in a separate Kotlin/Native process
            advanced("nativeGCAfterIteration", "true") // Trigger garbage collection after each iteration in Kotlin/Native
        }
    }
    targets {
        register("jvm") {
            jmhVersion = "1.21"
        }
        register("js")
        register("native")
        register("wasm")
    }
}
```

With this guide, you should now be well-equipped to fine-tune your benchmarking process, ensuring you generate precise, reliable performance measurements every time.

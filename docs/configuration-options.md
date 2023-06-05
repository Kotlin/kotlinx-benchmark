# Configuring kotlinx-benchmark

kotlinx-benchmark offers a plethora of configuration options that enable you to customize your benchmarking setup according to your precise needs. This advanced guide provides an in-depth explanation of how to setup your benchmark configurations, alongside detailed insights into kotlinx's functionalities.

## Table of Contents

- [Step 1: Laying the Foundation – Establish Benchmark Targets](#step-1)
- [Step 2: Tailoring the Setup – Create Benchmark Configurations](#step-2)
- [Step 3: Fine-tuning Your Setup – Understanding and Setting Configuration Options](#step-3)
  - [Basic Configuration Options: The Essential Settings](#step-3a)
  - [Advanced Configuration Options: The Power Settings](#step-3b)

## Step 1: Laying the Foundation – Establish Benchmark Targets <a name="step-1"></a>

Your journey starts by defining the `benchmark` section within your `build.gradle` file. This section is your playground where you register the compilations you wish to run benchmarks on, within a `targets` subsection.

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

### Basic Configuration Options: The Essential Settings <a name="step-3a"></a>

| Option | Description | Default Value | Possible Values |
| --- | --- | --- | --- |
| `iterations` | Specifies the number of iterations for measurements. | - | Integer |
| `warmups` | Specifies the number of iterations for system warming, ensuring accurate measurements. | - | Integer |
| `iterationTime` | Specifies the duration for each iteration, both measurement and warm-up. | - | Integer |
| `iterationTimeUnit` | Specifies the unit for `iterationTime`. | Seconds | "ns", "μs", "ms", "s", "m", "h", "d" |
| `outputTimeUnit` | Specifies the unit for the results display. | - | "ns", "μs", "ms", "s", "m", "h", "d" |
| `mode` | Selects between "thrpt" for measuring the number of function calls per unit time or "avgt" for measuring the time per function call. | "thrpt" | "thrpt", "avgt" |
| `include("…")` | Applies a regular expression to include benchmarks that match the substring in their fully qualified names. | - | Regex pattern |
| `exclude("…")` | Applies a regular expression to exclude benchmarks that match the substring in their fully qualified names. | - | Regex pattern |
| `param("name", "value1", "value2")` | Assigns values to a public mutable property, annotated with `@Param`. | - | Any string values |
| `reportFormat` | Defines the benchmark report's format options. | "json" | "json", "csv", "scsv", "text" |

### Advanced Configuration Options: The Power Settings <a name="step-3b"></a>

Beyond the basics, kotlinx allows you to take a deep dive into platform-specific settings, offering more control over your benchmarks:

| Option | Platform | Description | Default Value | Possible Values |
| --- | --- | --- | --- | --- |
| `advanced("nativeFork", "value")` | Kotlin/Native | Executes iterations within the same process ("perBenchmark") or each iteration in a separate process ("perIteration"). | "perBenchmark" | "perBenchmark", "perIteration" |
| `advanced("nativeGCAfterIteration", "value")` | Kotlin/Native | Triggers garbage collection after each iteration when set to `true`. | `false` | `true`, `false` |
| `advanced("jvmForks", "value")` | Kotlin/JVM | Determines how many times the harness should fork. | "1" | "0" (no fork), "1", "definedByJmh" (JMH decides) |
| `advanced("jsUseBridge", "value")` | Kotlin/JS, Kotlin/Wasm | Disables the generation of benchmark bridges to stop inlining optimizations when set to `false`. | - | `true`, `false` |

Here's an example of how you can customize a benchmark configuration using these options:

```groovy
benchmark {
    configurations {
        main { 
            warmups = 20 // Number of warmup iterations
            iterations = 10 // Number of measurement iterations
            iterationTime = 3 // Duration per iteration in seconds
            iterationTimeUnit = "s" // Unit for iterationTime
            mode = "avgt" // Measure the average time per function call
            outputTimeUnit = "ms" // Display results in milliseconds
            include(".*MyBenchmark.*") // Only include benchmarks matching this pattern
            param("size", "100", "200") // Parameter for benchmark
            reportFormat = "json" // Format of the benchmark report
        }
        smoke {
            warmups = 5 
            iterations = 3
            iterationTime = 500
            iterationTimeUnit = "ms"
            advanced("nativeFork", "perIteration")
            advanced("nativeGCAfterIteration", "true")
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

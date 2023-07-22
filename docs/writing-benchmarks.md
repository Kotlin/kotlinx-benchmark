# Writing Benchmarks with kotlinx-benchmark

This guide provides a walkthrough on writing benchmarks using the toolkit. It covers the necessary annotations, their purposes, and how to apply them to create effective benchmarks in Kotlin.

## Available Annotations

The toolkit offers various annotations that can be used to customize your setup. Here's an overview of these annotations:

| Annotation/Class | Description | Target |
|------------------|-------------|--------|
| `@Setup` | Denote methods that initialize resources or conditions prior to the execution of the benchmark | Function |
| `@TearDown` | Designate methods that carry out cleanup activities or resource deallocation after the benchmark has been executed. | Function |
| `@Benchmark` | Marks methods as microbenchmarks. | Function |
| `@State` | Groups benchmarks that share the same state. | Class |
| `@BenchmarkMode` | Determines the benchmark mode - `Throughput` (operations per second) or `AverageTime` (time per operation). | Class |
| `@OutputTimeUnit` | Sets the time unit for benchmark output. | Class |
| `@Warmup` | Defines warm-up phase properties, ensuring benchmark runs at full speed for measurement. | Class |
| `@Measurement` | Sets properties for the benchmark measurement phase. | Class |
| `@Param` | Defines parameters for benchmark customization. | Class |
| `@Scope` | Specifies when a new instance of the state object is provided. | Class |
| `@BenchmarkTimeUnit` | Specifies the unit of benchmark result display. | Class |
| `@Fork` | Allows the launch of separate JVM processes for each benchmark test | Class |
| `@NativeFork` | (Native Only) Defines isolation level for native benchmarks - `PerBenchmark` (new process for each benchmark) or `PerIteration` (new process for each iteration). | Class |

## Writing a Benchmark Class

Here's a step-by-step process to create a benchmark class:

1. Annotate the class with `@State(Scope.Benchmark)`. This indicates that it's a benchmark class:

    ```kotlin
    @State(Scope.Benchmark)
    class MyBenchmark {

    }
    ```

2. Inside the class, define any setup or teardown methods using `@Setup` and `@TearDown` annotations respectively. These methods are run before and after the entire benchmark run:

    ```kotlin
    @Setup
    fun prepare() {

    }

    @TearDown
    fun cleanup() {

    }
    ```

3. Customize your benchmark configuration using annotations like `@OutputTimeUnit`, `@Warmup`, and more if desired:

    ```kotlin
    @State(Scope.Benchmark)
    @Fork(1)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Warmup(iterations = WARMUP_ITERATIONS, time = 1, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
    class MyBenchmark {
    
        @Setup
        fun prepare() {

        }
        
        @Benchmark
        fun benchmarkMethod() {

        }
    
        @TearDown
        fun cleanup() {

        }
    }
    ```
    Note: Annotations will override all benchmark configuration values.

4. Define the method(s) to be benchmarked and annotate them with `@Benchmark`:

    ```kotlin
    @State(Scope.Benchmark)
    @Fork(1)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Warmup(iterations = WARMUP_ITERATIONS, time = 1, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
    fun benchmarkMethod() {

    }
    ```

    A final benchmark class would look something like:
    
    ```kotlin
    @State(Scope.Benchmark)
    class MyBenchmark {
    
        @Setup
        fun prepare() {

        }
    
        @Benchmark
        fun benchmarkMethod() {

        }
    
        @TearDown
        fun cleanup() {

        }
    }
    ```

For a deeper dive with hands-on examples, refer to our [examples](../examples) subproject.

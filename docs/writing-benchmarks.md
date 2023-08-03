## Writing Benchmarks

To get started, let's look at a simple example:

```kotlin
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@BenchmarkTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
class ExampleBenchmark {

    @Param("4", "10")
    var param: Int = 0

    private var list: MutableList<Int> = ArrayList()

    @Setup
    fun prepare() {
        list = MutableList(param) { it }
    }

    @Benchmark
    fun benchmarkMethod(): Int {
        return list.sum()
    }

    @TearDown
    fun cleanup() {
        list.clear()
    }
}
```

### Explaining the Annotations

#### @State

The `@State` annotation, when set to `Scope.Benchmark`, is applied to a class to represent that this class is responsible for holding the state or data for your benchmark tests. This class instance is shared among all benchmark threads, creating a consistent environment across all warmups, iterations, and measured executions. It helps in managing stateful operations in a multi-threaded context, providing an efficient way of handling state that's thread-safe and consistent across multiple runs. This ensures accurate and reliable benchmarking results, as the shared state remains the same throughout all the tests, preventing any discrepancies that could affect the outcomes. In the Kotlin/JVM and Java taregts, apart from `Scope.Benchmark`, you also have access to `Scope.Thread` and `Scope.Group`. `Scope.Thread` ensures each thread has its own unique state instance, while `Scope.Group` allows state sharing within a benchmark thread group. However, for other target environments, only `Scope.Benchmark` is supported, limiting the scope options to a single instance that is shared among all threads. In our snippet, the ExampleBenchmark class uses the @State(Scope.Benchmark) annotation, indicating that the state in this class is shared across all benchmark threads.

#### @Setup

The `@Setup` annotation is used to mark a method that sets up the necessary preconditions for your benchmark test. This method, annotated with `@Setup`, is executed before the actual benchmark methods are run. It serves as a preparatory step where you set up the environment for the benchmark, performing tasks such as generating data, establishing database connections, or preparing any other resources your benchmark requires. The key point to remember is that the `@Setup` method's execution time is not included in the final benchmark results - the timer starts only when the `@Benchmark` method begins. This makes `@Setup` an ideal place for initialization tasks that should not impact the timing results of your benchmark. By using the `@Setup` annotation, you ensure consistency across all executions of your benchmark, providing accurate and reliable results. In the provided example, the `@Setup` annotation is used to prepare a list containing integers from 0 to a specified parameter value.

#### @Benchmark

The `@Benchmark` annotation is used to specify the methods that you want to measure the performance of. Basically, it's the actual test you're running. The code you want to benchmark goes inside this method. In our example, the `benchmarkMethod` function is annotated with `@Benchmark`, which means the toolkit will measure the performance of the operation of summing all the integers in the list.

#### @TearDown

The `@TearDown` annotation is used to denote a method that's executed after the benchmarking method(s), typically responsible for cleaning up or deallocating any resources or conditions that were initialized or consumed during the benchmark. For instance, if your benchmark creates temporary files or opens network connections, the method marked with `@TearDown` is where you would put the code to delete those files or close those connections. The `@TearDown` annotation helps you avoid performance bias and ensure the proper maintenance of resources and the preparation of a clean environment for the next run. In our example, the `cleanup` function annotated with `@TearDown` is used to clear the list of integers after each benchmark iteration, ensuring that each test starts with the same initial state.

#### @BenchmarkMode

The `@BenchmarkMode` annotation sets the mode of operation for the benchmark. Applying the `@BenchmarkMode` annotation requires specifying a mode from the `Mode` enum, which includes several options. `Mode.Throughput` measures the raw throughput of your code in terms of the number of operations it can perform per unit of time, such as operations per second. When this mode is selected, the toolkit runs your code in a loop for a set duration, then calculates and reports the operation throughput. Conversely, `Mode.AverageTime` is used when you're more interested in the average time it takes to execute an operation. Here, the toolkit runs your code multiple times, records the time each operation takes, and calculates and reports the average time. Without an explicit `@BenchmarkMode` annotation, the toolkit defaults to `Mode.Throughput`. In our example, `@BenchmarkMode(Mode.Throughput)` is used, meaning the benchmark focuses on the number of times the benchmark method can be executed per unit of time.

#### @OutputTimeUnit

The `@OutputTimeUnit` annotation dictates the time unit in which your results will be presented. This time unit can range from minutes to nanoseconds. If a piece of code executes within a few milliseconds, presenting the result in milliseconds or microseconds provides a more accurate and detailed measurement. Conversely, for operations with longer execution times, you might choose to display the output in microseconds, seconds, or even minutes. Essentially, the `@OutputTimeUnit` annotation is about enhancing the readability and interpretability of your benchmarks. If this annotation isn't specified, it defaults to using seconds as the time unit. In our example the OutputTimeUnit is set to milliseconds.

#### @Warmup

The `@Warmup` annotation is used to specify a preliminary phase before the actual benchmarking takes place. During this warmup phase, the code in your `@Benchmark` method, is executed several times, but these runs aren't included in the final benchmark results. The primary purpose of the warmup phase is to let the system "warm up" and reach its optimal performance state. In a typical scenario, when a Java application starts, the JVM (Java Virtual Machine) goes through a process called "JIT (Just-In-Time) compilation" where it learns about your code, optimizes it, and compiles it into native machine code for faster execution. The more the code is run, the more chances the JVM gets to optimize it, potentially making it run faster over time. The warmup phase is akin to giving the JVM a "practice run" to figure out the best optimizations for your code. This is particularly crucial for benchmarking because if you were to start measuring performance right from the first run, your results might be skewed by the JVM's initial learning and optimization process. In our example, the `@Warmup` annotation is used to allow five iterations, each lasting one second, of executing the benchmark method before the actual measurement starts.

#### @Measurement

The `@Measurement` annotation is used to control the properties of the actual benchmarking phase. It sets how many times the benchmark method is run (iterations) and how long each run should last. The results from these runs are recorded and reported as the final benchmark results. In our example, the `@Measurement` annotation specifies that the benchmark method will be run once for a duration of one second for the final performance measurement.

#### @Fork

The `@Fork` annotation is utilized to command to launch each benchmark in a standalone Java Virtual Machine (JVM) process. The JVM conducts various behind-the-scenes optimizations such as Just-In-Time compilation, class loading, and garbage collection. These can significantly impact the performance of our code. However, these influences might differ from one run to another, leading to inconsistent or misleading benchmark results if multiple benchmarks are executed within the same JVM process. By triggering the JVM to fork for each benchmark, these JVM-specific factors are eliminated from affecting the benchmark results, providing a clean and independent environment for each benchmark, enhancing the reliability and comparability of results. The value you assign to the `@Fork` annotation determines the number of separate JVM processes initiated for each benchmark. If this annotation is not specified, JVMs are not forked and all benchmarks are run in the same JVM process. Repeating the benchmark across multiple JVMs and averaging the results gives a more accurate representation of typical performance and accommodates for variability possibly caused by different JVM startups. In our example, the `@Fork(1)` annotation indicates that each benchmark test should run in one separate JVM process, thus ensuring an isolated and reliable testing environment for each test run.

#### @Param

The `@Param` annotation is used to pass different parameters to your benchmark method. It allows you to run the same benchmark method with different input values, so you can see how these variations affect performance. The values you provide for the `@Param` annotation are the different inputs you want to use in your benchmark test. The benchmark will run once for each provided value. In our example, `@Param` annotation is used with values '1' and '2', which means the benchmarkMethod will be executed twice, once with the `param` value as '1' and then with '2'. This could serve to help in understanding how the size of the input list impacts the time it takes to sum all the integers in the list.

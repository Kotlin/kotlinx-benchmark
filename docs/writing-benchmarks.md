# Writing Benchmarks

If you're familiar with the Java Microbenchmark Harness (JMH) toolkit, you'll find that the `kotlinx-benchmark`
library shares a similar approach to crafting benchmarks. This compatibility allows you to seamlessly run your
JMH benchmarks written in Kotlin on various platforms with minimal, if any, modifications.

Like JMH, kotlinx-benchmark is annotation-based, meaning you configure benchmark execution behavior using annotations.
The library then extracts metadata provided through annotations to generate code that benchmarks the specified code
in the desired manner.

To get started, let's examine a simple example of a multiplatform benchmark:

```kotlin
import kotlinx.benchmark.*

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@Warmup(iterations = 10, time = 500, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@Measurement(iterations = 20, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
@State(Scope.Benchmark)
class ExampleBenchmark {

    // Parameterizes the benchmark to run with different list sizes
    @Param("4", "10")
    var size: Int = 0

    private val list = ArrayList<Int>()

    // Prepares the test environment before each benchmark run
    @Setup
    fun prepare() {
        for (i in 0..<size) {
            list.add(i)
        }
    }

    // Cleans up resources after each benchmark run
    @TearDown
    fun cleanup() {
        list.clear()
    }

    // The actual benchmark method
    @Benchmark
    fun benchmarkMethod(): Int {
        return list.sum()
    }
}
```

**Example Description**:
This example tests the speed of summing numbers in an `ArrayList`. We evaluate this operation with lists
of 4 and 10 numbers to understand the method's performance with different list sizes.

## Explaining the Annotations

The following annotations are available to define and fine-tune your benchmarks.

### @State

The `@State` annotation specifies the extent to which the state object is shared among the worker threads,
and it is mandatory for benchmark classes to be marked with this annotation to define their scope of state sharing.

Currently, multi-threaded execution of a benchmark method is supported only on the JVM, where you can specify various scopes.
Refer to [JMH documentation of Scope](https://javadoc.io/doc/org.openjdk.jmh/jmh-core/latest/org/openjdk/jmh/annotations/Scope.html)
for details about available scopes and their implications.
In non-JVM targets, only `Scope.Benchmark` is applicable.

When writing JVM-only benchmarks, benchmark classes are not required to be annotated with `@State`.
Refer to [JMH documentation of @State](https://javadoc.io/doc/org.openjdk.jmh/jmh-core/latest/org/openjdk/jmh/annotations/State.html)
for details about the effect and restrictions of the annotation in Kotlin/JVM.

In our snippet, the `ExampleBenchmark` class is annotated with `@State(Scope.Benchmark)`,
indicating the state is shared across all worker threads.

### @Setup

The `@Setup` annotation marks a method that sets up the necessary preconditions for your benchmark test.
It serves as a preparatory step where you initiate the benchmark environment.

The setup method is executed once before the entire set of iterations for a benchmark method begins.
In Kotlin/JVM, you can specify when the setup method should be executed, e.g., `@Setup(Level.Iteration)`.
Refer to [JMH documentation of Level](https://javadoc.io/doc/org.openjdk.jmh/jmh-core/latest/org/openjdk/jmh/annotations/Level.html)
for details about available levels in Kotlin/JVM.

The key point to remember is that the `@Setup` method's execution time is not included in the final benchmark
results - the timer starts only when the `@Benchmark` method begins. This makes `@Setup` an ideal place
for initialization tasks that should not impact the timing results of your benchmark.

The method annotated with `@Setup` should be `public` and have no arguments.
In Kotlin/JVM, these restrictions are slightly less strict.
Refer to [JMH documentation of @Setup](https://javadoc.io/doc/org.openjdk.jmh/jmh-core/latest/org/openjdk/jmh/annotations/Setup.html)
for details about the effect and restrictions of the annotation in Kotlin/JVM.

In the provided example, the `@Setup` annotation is used to populate an `ArrayList` with integers from `0` up to a specified `size`.

### @TearDown

The `@TearDown` annotation is used to denote a method that resets and cleans up the benchmarking environment.
It is chiefly responsible for the cleanup or deallocation of resources and conditions set up in the `@Setup` method.

The teardown method is executed once after the entire iteration set of a benchmark method.
In Kotlin/JVM, you can specify when the teardown method should be executed, e.g., `@TearDown(Level.Iteration)`.
Refer to [JMH documentation of Level](https://javadoc.io/doc/org.openjdk.jmh/jmh-core/latest/org/openjdk/jmh/annotations/Level.html)
for details about available levels in Kotlin/JVM. 

The `@TearDown` annotation is crucial for avoiding performance bias, ensuring the proper maintenance of resources,
and preparing a clean environment for the next run. Similar to the `@Setup` method, the execution time of the
`@TearDown` method is not included in the final benchmark results.

The method annotated with `@TearDown` should be `public` and have no arguments.
In Kotlin/JVM, these restrictions are slightly less strict.
Refer to [JMH documentation of @TearDown](https://javadoc.io/doc/org.openjdk.jmh/jmh-core/latest/org/openjdk/jmh/annotations/TearDown.html)
for more information on the effect and restrictions of the annotation in Kotlin/JVM.

In our example, the `cleanup` function annotated with `@TearDown` is used to clear our `ArrayList`.

### @Benchmark

The `@Benchmark` annotation is used to specify the methods that you want to measure the performance of.
It's the actual test you're running. The code you want to benchmark goes inside this method.
All other annotations are employed to configure the benchmark's environment and execution.

Benchmark methods may include only a single [Blackhole](#blackhole) type as an argument, or have no arguments at all.
It's important to note that benchmark methods should be `public`. In Kotlin/JVM, these restrictions are slightly less strict. 
Refer to [JMH documentation of @Benchmark](https://javadoc.io/doc/org.openjdk.jmh/jmh-core/latest/org/openjdk/jmh/annotations/Benchmark.html)
for details about restrictions for benchmark methods in Kotlin/JVM.

In our example, the `benchmarkMethod` function is annotated with `@Benchmark`,
which means the toolkit will measure the performance of the operation of summing all the integers in the list.

### @BenchmarkMode

The `@BenchmarkMode` annotation sets the mode of operation for the benchmark.

Applying the `@BenchmarkMode` annotation requires specifying a mode from the `Mode` enum.
`Mode.Throughput` measures the raw throughput of your code in terms of the number of operations it can perform per unit
of time, such as operations per second. `Mode.AverageTime` is used when you're more interested in the average time it
takes to execute an operation. Without an explicit `@BenchmarkMode` annotation, the toolkit defaults to `Mode.Throughput`.
In Kotlin/JVM, the `Mode` enum has a few more options, including `SingleShotTime`.
Refer to [JMH documentation of Mode](https://javadoc.io/doc/org.openjdk.jmh/jmh-core/latest/org/openjdk/jmh/annotations/Mode.html)
for details about available options in Kotlin/JVM.

The annotation is put at the enclosing class and has the effect over all `@Benchmark` methods in the class.
In Kotlin/JVM, it may be put at `@Benchmark` method to have effect on that method only.
Refer to [JMH documentation of @BenchmarkMode](https://javadoc.io/doc/org.openjdk.jmh/jmh-core/latest/org/openjdk/jmh/annotations/BenchmarkMode.html)
for details about the effect of the annotation in Kotlin/JVM.

In our example, `@BenchmarkMode(Mode.AverageTime)` is used, indicating that the benchmark aims to measure the
average execution time of the benchmark method.

### @OutputTimeUnit

The `@OutputTimeUnit` annotation specifies the time unit in which your results will be presented.
This time unit can range from minutes to nanoseconds. If a piece of code executes within a few milliseconds,
presenting the result in nanoseconds or microseconds provides a more accurate and detailed measurement.
Conversely, for operations with longer execution times, you might choose to display the output in milliseconds, seconds, or even minutes.
Essentially, the `@OutputTimeUnit` annotation enhances the readability and interpretability of benchmark results.
By default, if the annotation is not specified, results are presented in seconds.

The annotation is put at the enclosing class and has the effect over all `@Benchmark` methods in the class.
In Kotlin/JVM, it may be put at `@Benchmark` method to have effect on that method only.
Refer to [JMH documentation of @OutputTimeUnit](https://javadoc.io/doc/org.openjdk.jmh/jmh-core/latest/org/openjdk/jmh/annotations/OutputTimeUnit.html)
for details about the effect of the annotation in Kotlin/JVM.

In our example, the `@OutputTimeUnit` is set to milliseconds.

### @Warmup

The `@Warmup` annotation specifies a preliminary phase before the actual benchmarking takes place.
During this warmup phase, the code in your `@Benchmark` method is executed several times, but these runs aren't included
in the final benchmark results. The primary purpose of the warmup phase is to let the system "warm up" and reach its
optimal performance state so that the results of measurement iterations are more stable.

The annotation is put at the enclosing class and has the effect over all `@Benchmark` methods in the class.
In Kotlin/JVM, it may be put at `@Benchmark` method to have effect on that method only.
Refer to [JMH documentation of @Warmup](https://javadoc.io/doc/org.openjdk.jmh/jmh-core/latest/org/openjdk/jmh/annotations/Warmup.html)
for details about the effect of the annotation in Kotlin/JVM.

In our example, the `@Warmup` annotation is used to allow 10 iterations of executing the benchmark method before
the actual measurement starts. Each iteration lasts 500 milliseconds.

### @Measurement

The `@Measurement` annotation controls the properties of the actual benchmarking phase.
It sets how many iterations the benchmark method is run and how long each run should last.
The results from these runs are recorded and reported as the final benchmark results.

The annotation is put at the enclosing class and has the effect over all `@Benchmark` methods in the class.
In Kotlin/JVM, it may be put at `@Benchmark` method to have effect on that method only.
Refer to [JMH documentation of @Measurement](https://javadoc.io/doc/org.openjdk.jmh/jmh-core/latest/org/openjdk/jmh/annotations/Measurement.html)
for details about the effect of the annotation in Kotlin/JVM.

In our example, the `@Measurement` annotation specifies that the benchmark method will run 20 iterations,
with each iteration lasting one second, for the final performance measurement.

### @Param

The `@Param` annotation is used to pass different parameters to your benchmark method.
It allows you to run the same benchmark method with different input values, so you can see how these variations affect
performance.

The values provided by the `@Param` annotation represent the different inputs you want to use in your benchmark.
Since the benchmark runs once for each provided value, the annotation should have at least one argument.
The annotation values are given in `String` and will be coerced as needed to match the property type.

The property marked with this annotation should be mutable (`var`) and `public`.
Additionally, only properties of primitive types or the `String` type can be annotated with `@Param`.
In Kotlin/JVM, these restrictions are slightly less strict.
Refer to [JMH documentation of @Param](https://javadoc.io/doc/org.openjdk.jmh/jmh-core/latest/org/openjdk/jmh/annotations/Param.html)
for details about the effect and restrictions of the annotation in Kotlin/JVM.

In our example, the `@Param` annotation is used with values `"4"` and `"10"`, meaning the `benchmarkMethod`
will be benchmarked twice - once with the `size` value set to `4` and then with `10`.
This approach helps in understanding how the input list's size affects the time taken to sum its integers.

### Other JMH annotations

In Kotlin/JVM, you can use annotations provided by JMH to further tune your benchmarks execution behavior.
Refer to [JMH documentation](https://javadoc.io/doc/org.openjdk.jmh/jmh-core/latest/org/openjdk/jmh/annotations/package-summary.html)
for available annotations.

## Blackhole

Modern compilers often eliminate computations they find unnecessary, which can distort benchmark results.
In essence, `Blackhole` maintains the integrity of benchmarks by preventing unwanted optimizations such as dead-code
elimination by the compiler or the runtime virtual machine. A `Blackhole` should be used when the benchmark produces several values.
If the benchmark produces a single value, just return it. It will be implicitly consumed by a `Blackhole`.

### How to Use Blackhole:

Inject `Blackhole` into your benchmark method and use it to consume results of your computations:

```kotlin
@Benchmark
fun iterateBenchmark(bh: Blackhole) {
    for (e in myList) {
        bh.consume(e)
    }
}
```

By consuming results, you signal to the compiler that these computations are significant and shouldn't be optimized away.

For a deeper dive into `Blackhole` and its nuances in JVM, you can refer to:
- [Official Javadocs](https://javadoc.io/doc/org.openjdk.jmh/jmh-core/latest/org/openjdk/jmh/infra/Blackhole.html)
- [JMH](https://github.com/openjdk/jmh/blob/1.37/jmh-core/src/main/java/org/openjdk/jmh/infra/Blackhole.java#L157-L254)
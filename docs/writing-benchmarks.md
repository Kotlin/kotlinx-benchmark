## Writing Benchmarks

To get started, let's look at a simple multiplatform example:

```kotlin
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 20, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 20, time = 1, timeUnit = TimeUnit.SECONDS)
@BenchmarkTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
class ExampleBenchmark {

    @Param("4", "10")
    var size: Int = 0

    private val list = ArrayList<Int>()

    @Setup
    fun prepare() {
        for (i in 0 until size) {
            list.add(i)
        }
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

**Example Description**:
Our example tests the speed of summing numbers in an ArrayList. We try it with a list of 4 numbers and then with a list of 10 numbers.
This helps us determine the efficiency of our summing method with different list sizes.

### Explaining the Annotations

#### @State

The `@State` annotation is used to mark benchmark classes.
In the Kotlin/JVM target, however, benchmark classes are not required to be annotated with `@State`.
In the Kotlin/JVM target, you can specify to which extent the state object is shared among the worker threads, e.g, `@State(Scope.Group)`.
Refer to [JMH documentation](https://javadoc.io/doc/org.openjdk.jmh/jmh-core/latest/org/openjdk/jmh/annotations/Scope.html)
for details about available scopes. Multi-threaded execution of a benchmark method is not supported in other Kotlin targets,
thus only `Scope.Benchmark` is available.
In our snippet, the ExampleBenchmark class is marked with `@State(Scope.Benchmark)`,
indicating that performance of benchmark methods in this class should be measured.

#### @Setup

The `@Setup` annotation is used to mark a method that sets up the necessary preconditions for your benchmark test.
It serves as a preparatory step where you set up the environment for the benchmark.
In the Kotlin/JVM target, you can specify when the setup method should be executed, e.g, `@Setup(Level.Iteration)`.
Refer to [JMH documentation](https://javadoc.io/doc/org.openjdk.jmh/jmh-core/latest/org/openjdk/jmh/annotations/Level.html)
for details about available levels. In other targets it operates always on the `Trial` level, that is, the setup method is
executed once before the entire set of benchmark method iterations. The key point to remember is that the `@Setup`
method's execution time is not included in the final benchmark results - the timer starts only when the `@Benchmark`
method begins. This makes `@Setup` an ideal place for initialization tasks that should not impact the timing results of your benchmark.
In the provided example, the `@Setup` annotation is used to populate an ArrayList with integers from 0 up to a specified size.

#### @TearDown

The `@TearDown` annotation is used to denote a method that's executed after the benchmarking method(s).
This method is typically responsible for cleaning up or deallocating any resources or conditions that were initialized in the `@Setup` method.
In the Kotlin/JVM target, you can specify when the tear down method should be executed, e.g, `@TearDown(Level.Iteration)`.
Refer to [JMH documentation](https://javadoc.io/doc/org.openjdk.jmh/jmh-core/latest/org/openjdk/jmh/annotations/Level.html)
for details about available levels. In other targets it operates always on `Trial` level, that is, the tear down method
is executed once after the entire set of benchmark method iterations. The `@TearDown` annotation helps you avoid
performance bias and ensure the proper maintenance of resources and the preparation of a clean environment for the next run.
As with the `@Setup` method, the `@TearDown` method's execution time is not included in the final benchmark results.
In our example, the `cleanup` function annotated with `@TearDown` is used to clear our ArrayList.

#### @Benchmark

The `@Benchmark` annotation is used to specify the methods that you want to measure the performance of.
Basically, it's the actual test you're running. It's important to note that the benchmark methods must always be public.
The code you want to benchmark goes inside this method.
In our example, the `benchmarkMethod` function is annotated with `@Benchmark`,
which means the toolkit will measure the performance of the operation of summing all the integers in the list.

#### @BenchmarkMode

The `@BenchmarkMode` annotation sets the mode of operation for the benchmark.
Applying the `@BenchmarkMode` annotation requires specifying a mode from the `Mode` enum, which includes several options.
`Mode.Throughput` measures the raw throughput of your code in terms of the number of operations it can perform per unit
of time, such as operations per second. `Mode.AverageTime` is used when you're more interested in the average time it
takes to execute an operation. Without an explicit `@BenchmarkMode` annotation, the toolkit defaults to `Mode.Throughput`.
In our example, `@BenchmarkMode(Mode.Throughput)` is used, meaning the benchmark focuses on the number of times the
benchmark method can be executed per unit of time.

#### @OutputTimeUnit

The `@OutputTimeUnit` annotation specifies the time unit in which your results will be presented.
This time unit can range from minutes to nanoseconds. If a piece of code executes within a few milliseconds,
presenting the result in milliseconds or microseconds provides a more accurate and detailed measurement.
Conversely, for operations with longer execution times, you might choose to display the output in microseconds, seconds, or even minutes.
Essentially, the `@OutputTimeUnit` annotation is about enhancing the readability and interpretability of benchmark results.
If this annotation isn't specified, it defaults to using seconds as the time unit.
In our example, the OutputTimeUnit is set to milliseconds.

#### @Warmup

The `@Warmup` annotation is used to specify a preliminary phase before the actual benchmarking takes place.
During this warmup phase, the code in your `@Benchmark` method is executed several times, but these runs aren't included
in the final benchmark results. The primary purpose of the warmup phase is to let the system "warm up" and reach its
optimal performance state so that the results of measurement iterations are more stable.
In our example, the `@Warmup` annotation is used to allow 20 iterations, each lasting one second,
of executing the benchmark method before the actual measurement starts.

#### @Measurement

The `@Measurement` annotation is used to control the properties of the actual benchmarking phase.
It sets how many iterations the benchmark method is run and how long each run should last.
The results from these runs are recorded and reported as the final benchmark results.
In our example, the `@Measurement` annotation specifies that the benchmark method will be run 20 iterations
for a duration of one second for the final performance measurement.

#### @Param

The `@Param` annotation is used to pass different parameters to your benchmark method.
It allows you to run the same benchmark method with different input values, so you can see how these variations affect
performance. The values you provide for the `@Param` annotation are the different inputs you want to use in your
benchmark test. The benchmark will run once for each provided value.
The property marked with this annotation must be public and mutable (`var`).
In our example, `@Param` annotation is used with values '4' and '10', which means the benchmarkMethod will be executed
twice, once with the `param` value as '4' and then with '10'. This helps to understand how the input list's size affects the time taken to sum its integers.

#### Other JMH annotations

In a Kotlin/JVM target, you can use annotations provided by JMH to further tune your benchmarks execution behavior.
Refer to [JMH documentation](https://javadoc.io/doc/org.openjdk.jmh/jmh-core/latest/org/openjdk/jmh/annotations/package-summary.html) for available annotations.

## Blackhole

Modern compilers often eliminate computations they find unnecessary, which can distort benchmark results.
In essence, `Blackhole` maintains the integrity of benchmarks by preventing unwanted optimizations such as dead-code
elimination by the compiler or the runtime virtual machine. A `Blackhole` is used when the benchmark produces several values.
If the benchmark produces a single value, just return it. It will be implicitly consumed by a `Blackhole`.

#### How to Use Blackhole:

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
- [Official Javadocs](https://javadoc.io/static/org.openjdk.jmh/jmh-core/1.23/org/openjdk/jmh/infra/Blackhole.html)
- [JMH](https://github.com/openjdk/jmh/blob/1.37/jmh-core/src/main/java/org/openjdk/jmh/infra/Blackhole.java#L157-L254)
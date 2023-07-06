# kotlinx-benchmark Examples Guide

This guide is specifically designed for experienced Kotlin developers. It aims to help you smoothly navigate and run the benchmark examples included in this repository.

## Getting Started

To begin, you'll need to clone the `kotlinx-benchmark` repository to your local machine:

```
git clone https://github.com/Kotlin/kotlinx-benchmark.git
```

## Running the Examples

Each example in this repository is an autonomous project, encapsulated in its own environment. Reference the [tasks-overview](../docs/tasks-overview.md) for a detailed list and explanation of available tasks.

To execute all benchmarks for a specific example, you'll use the following command structure:

```
./gradlew :examples:[example-name]:benchmark
```

Here, `[example-name]` is the name of the example you wish to benchmark. For instance, to run benchmarks for the `kotlin-kts` example, the command would be:

```
./gradlew :examples:kotlin-kts:benchmark
```

This pattern applies to all examples in the repository.

## Troubleshooting

In case of any issues encountered while setting up or running the benchmarks, verify that you're executing commands from the correct directory. For persisting issues, don't hesitate to open an [issue](https://github.com/Kotlin/kotlinx-benchmark/issues).

Happy benchmarking!

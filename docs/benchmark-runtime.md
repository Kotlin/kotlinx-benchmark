# kotlinx.benchmark: A Comprehensive Guide to Benchmark Runtime for Each Target

This document provides an in-depth overview of the kotlinx.benchmark library, focusing on how the benchmark runtime works for each supported target: JVM, JavaScript, and Native. This guide is designed for beginners and intermediates, providing a clear understanding of the underlying libraries used and the benchmark execution process.

## Table of Contents

- [JVM Target](#jvm-target)
- [JavaScript Target](#javascript-target)
- [Native Target](#native-target)

## JVM Target

The JVM target in kotlinx.benchmark leverages the Java Microbenchmark Harness (JMH) to run benchmarks. JMH is a widely-used tool for building, running, and analyzing benchmarks written in Java and other JVM languages.

### Benchmark Execution

JMH handles the execution of benchmarks, managing the setup, running, and teardown of tests. It also handles the calculation of results, providing a robust and reliable framework for benchmarking on the JVM.

### Benchmark Configuration

The benchmark configuration is handled through annotations that map directly to JMH annotations. These include `@State`, `@Benchmark`, `@BenchmarkMode`, `@OutputTimeUnit`, `@Warmup`, `@Measurement`, and `@Param`.

### File Operations

File reading and writing operations are performed using standard Java I/O classes, providing a consistent and reliable method for file operations across all JVM platforms.

# Table of Contents
1. [Introduction](#Understanding-Benchmark-Runtime-Across-Targets)
2. [JVM: Harnessing JMH](#jvm-harnessing-jmh)
3. [JavaScript: Benchmark.js Integration and In-built Support](#javascript-benchmarkjs-integration-and-in-built-support)
4. [Native: Harnessing Native Capabilities](#native-harnessing-native-capabilities)
5. [WebAssembly (Wasm): Custom-Built Benchmarking](#webassembly-wasm-custom-built-benchmarking)

# Understanding Benchmark Runtime Across Targets

This comprehensive guide aims to shed light on the underlying libraries that Kotlinx Benchmark utilizes to measure performance on these platforms, and elucidate the benchmark runtime process.

## JVM: Harnessing JMH
In the JVM ecosystem, Kotlinx Benchmark capitalizes on the Java microbenchmarking harness [JMH](https://openjdk.org/projects/code-tools/jmh/). Designed by OpenJDK, JMH is a well-respected tool for creating, executing, and scrutinizing nano/micro/milli/macro benchmarks composed in Java and other JVM-compatible languages.

Kotlinx Benchmark complements JMH with an array of advanced features that fine-tune JVM-specific settings. An exemplary feature is the handling of 'forks', a mechanism that facilitates running multiple tests in distinct JVM processes. By doing so, it assures a pristine environment for each test, enhancing the reliability of the benchmark results. Moreover, its sophisticated error and exception handling system ensures that any issues arising during testing are logged and addressed.

## JavaScript: Benchmark.js Integration and In-built Support
Targeting JavaScript, Kotlinx Benchmark utilizes the `benchmark.js` library to measure performance. Catering to both synchronous and asynchronous benchmarks, this library enables evaluation of a vast array of JavaScript operations. `benchmark.js` operates by setting up a suite of benchmarks, where each benchmark corresponds to a distinct JavaScript operation to be evaluated. 

It's noteworthy that, alongside `benchmark.js`, Kotlinx Benchmark also incorporates its own built-in yet somewhat limited benchmarking system for Kotlin/JavaScript runtime.

## Native: Harnessing Native Capabilities
For Native platforms, Kotlinx Benchmark resorts to its built-in benchmarking system, which is firmly rooted in platform-specific technologies. Benchmarks are defined in the form of suites, each representing a specific Kotlin/Native operation to be evaluated.

## WebAssembly (Wasm): Custom-Built Benchmarking
For Kotlin code running on WebAssembly (Wasm), Kotlinx Benchmark deploys built-in mechanisms to establish a testing milieu and measure code performance.

In this setup, similarly a suite of benchmarks is created, each pinpointing a different code segment. The execution time of each benchmark is gauged using high-resolution JavaScript functions, thereby providing accurate and precise performance measurements.
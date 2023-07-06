# Kotlin-Multiplatform Example

[![Open in GitHub Codespaces](https://github.com/codespaces/badge.svg)](https://codespaces.new/Kotlin/kotlinx-benchmark)

## Project Structure

Inside of this example, you'll see the following folders and files:

```
│   build.gradle  ==> Build configuration file for Gradle
│
└───src  ==> Source code root
    ├───commonMain  ==> Shared code
    │   └───kotlin  
    │       │   CommonBenchmark.kt  ==> Common benchmarks
    │       │   InheritedBenchmark.kt  ==> Inherited benchmarks
    │       │   ParamBenchmark.kt  ==> Parameterized benchmarks
    │       │
    │       └───nested  ==> Nested benchmarks
    │               CommonBenchmark.kt
    │
    ├───jsMain  ==> JavaScript-specific code
    │   └───kotlin
    │           JsAsyncBenchmarks.kt  ==> JS async benchmarks
    │           JsTestBenchmark.kt  ==> JS benchmarks
    │
    ├───jvmBenchmark  ==> JVM-specific benchmarks
    │   └───kotlin
    │           JvmBenchmark.kt
    │
    ├───jvmMain  ==> JVM-specific code
    │   └───kotlin
    │           JvmTestBenchmark.kt  ==> JVM benchmarks
    │
    ├───nativeMain  ==> Native-specific code
    │   └───kotlin
    │           NativeTestBenchmark.kt  ==> Native benchmarks
    │
    └───wasmMain  ==> WebAssembly-specific code
        └───kotlin
                WasmTestBenchmark.kt  ==> WebAssembly benchmarks
```

## Tasks

All tasks can be run from the root of the library, from a terminal:

| Task Name | Action |
| --- | --- |
| `assembleBenchmarks` | Generates and builds all benchmarks in the project. |
| `benchmark` | Executes all benchmarks in the project. |
| `compileJsIrBenchmarkKotlinJsIr` | Compiles the source files for 'jsIr' benchmark. |
| `compileJsIrBuiltInBenchmarkKotlinJsIrBuiltIn` | Compiles the source files for 'jsIrBuiltIn' benchmark. |
| `compileWasmBenchmarkKotlinWasm` | Compiles the source files for 'wasm' benchmark. |
| `csvBenchmark` | Executes all benchmarks in the project with the CSV configuration. |
| `fastBenchmark` | Executes all benchmarks in the project with the Fast configuration. |
| `forkBenchmark` | Executes all benchmarks in the project with the Fork configuration. |
| `jsIrBenchmark` | Executes benchmark for the 'jsIr' source set. |
| `jsIrBenchmarkGenerate` | Generates source files for the 'jsIr' source set. |
| `jsIrBuiltInBenchmark` | Executes benchmark for the 'jsIrBuiltIn' source set. |
| `jsIrBuiltInBenchmarkGenerate` | Generates source files for the 'jsIrBuiltIn' source set. |
| `jsIrBuiltInCsvBenchmark` | Executes benchmark for the 'jsIrBuiltIn' source set with the CSV configuration. |
| `jsIrBuiltInFastBenchmark` | Executes benchmark for the 'jsIrBuiltIn' source set with the Fast configuration. |
| `jsIrBuiltInForkBenchmark` | Executes benchmark for the 'jsIrBuiltIn' source set with the Fork configuration. |
| `jsIrBuiltInParamsBenchmark` | Executes benchmark for the 'jsIrBuiltIn' source set with the Params configuration. |
| `jsIrCsvBenchmark` | Executes benchmark for the 'jsIr' source set with the CSV configuration. |
| `jsIrFastBenchmark` | Executes benchmark for the 'jsIr' source set with the Fast configuration. |
| `jsIrForkBenchmark` | Executes benchmark for the 'jsIr' source set with the Fork configuration. |
| `jsIrParamsBenchmark` | Executes benchmark for the 'jsIr' source set with the Params configuration. |
| `jvmBenchmark` | Executes benchmark for the 'jvm' source set. |
| `jvmBenchmarkBenchmark` | Executes benchmark for the 'jvmBenchmark' source set. |
| `jvmBenchmarkBenchmarkCompile` | Compiles the source files for 'jvmBenchmark'. |
| `jvmBenchmarkBenchmarkGenerate` | Generates source files for the 'jvmBenchmark' source set. |
| `jvmBenchmarkBenchmarkJar` | Builds the JAR for 'jvmBenchmark' compiled files. |
| `jvmBenchmarkCompile` | Compiles the source files for the 'jvm' benchmark. |
| `jvmBenchmarkCsvBenchmark` | Executes benchmark for the 'jvmBenchmark' source set with the CSV configuration. |
| `jvmBenchmarkFastBenchmark` | Executes benchmark for the 'jvmBenchmark' source set with the Fast configuration. |
| `jvmBenchmarkForkBenchmark` | Executes benchmark for the 'jvmBenchmark' source set with the Fork configuration. |
| `jvmBenchmarkGenerate` | Generates source files for the 'jvm' source set. |
| `jvmBenchmarkJar` | Builds the JAR for 'jvm' compiled files. |
| `jvmBenchmarkParamsBenchmark` | Executes benchmark for the 'j| `jvmBenchmarkParamsBenchmark` | Executes benchmark for the 'jvmBenchmark' source set with the Params configuration. |
| `jvmCsvBenchmark` | Executes benchmark for the 'jvm' source set with the CSV configuration. |
| `jvmFastBenchmark` | Executes benchmark for the 'jvm' source set with the Fast configuration. |
| `jvmForkBenchmark` | Executes benchmark for the 'jvm' source set with the Fork configuration. |
| `jvmParamsBenchmark` | Executes benchmark for the 'jvm' source set with the Params configuration. |
| `linkNativeBenchmarkReleaseExecutableNative` | Compiles the source files for 'native' benchmark. |
| `nativeBenchmark` | Executes benchmark for the 'native' source set. |
| `nativeBenchmarkGenerate` | Generates source files for the 'native' source set. |
| `nativeCsvBenchmark` | Executes benchmark for the 'native' source set with the CSV configuration. |
| `nativeFastBenchmark` | Executes benchmark for the 'native' source set with the Fast configuration. |
| `nativeForkBenchmark` | Executes benchmark for the 'native' source set with the Fork configuration. |
| `nativeParamsBenchmark` | Executes benchmark for the 'native' source set with the Params configuration. |
| `paramsBenchmark` | Executes all benchmarks in the project with the Params configuration. |
| `wasmBenchmark` | Executes benchmark for the 'wasm' source set. |
| `wasmBenchmarkGenerate` | Generates source files for the 'wasm' source set. |
| `wasmCsvBenchmark` | Executes benchmark for the 'wasm' source set with the CSV configuration. |
| `wasmFastBenchmark` | Executes benchmark for the 'wasm' source set with the Fast configuration. |
| `wasmForkBenchmark` | Executes benchmark for the 'wasm' source set with the Fork configuration. |
| `wasmParamsBenchmark` | Executes benchmark for the 'wasm' source set with the Params configuration. |

## Want to learn more?

Feel free to engage in benchmarking discussions on the `#benchmarks` channel on [Kotlinlang Slack](https://kotlinlang.org/community/slack), explore the `kotlinx-benchmark` tagged questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/kotlinx-benchmark), or dive into the [kotlinx-benchmark Github Discussions](https://github.com/Kotlin/kotlinx-benchmark/discussions) for more insights and interactions.
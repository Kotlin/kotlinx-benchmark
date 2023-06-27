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

All tasks can be run from the root of the project, from a terminal:

| Task Name | Action |
| --- | --- |
| `gradle assembleBenchmarks` | Generate and build all benchmarks in the project |
| `gradle benchmark` | Execute all benchmarks in the project |
| `gradle compileJsIrBenchmarkKotlinJsIr` | Compile JS benchmark source files for 'jsIr' |
| `gradle compileJsIrBuiltInBenchmarkKotlinJsIrBuiltIn` | Compile JS benchmark source files for 'jsIrBuiltIn' |
| `gradle compileWasmBenchmarkKotlinWasm` | Compile Wasm benchmark source files for 'wasm' |
| `gradle csvBenchmark` | Execute all benchmarks in a project |
| `gradle fastBenchmark` | Execute all benchmarks in a project |
| `gradle forkBenchmark` | Execute all benchmarks in a project |
| `gradle jsIrBenchmark` | Executes benchmark for 'jsIr' with NodeJS |
| `gradle jsIrBenchmarkGenerate` | Generate JS source files for 'jsIr' |
| `gradle jsIrBuiltInBenchmark` | Executes benchmark for 'jsIrBuiltIn' with NodeJS |
| `gradle jsIrBuiltInBenchmarkGenerate` | Generate JS source files for 'jsIrBuiltIn' |
| `gradle jsIrBuiltInCsvBenchmark` | Executes benchmark for 'jsIrBuiltIn' with NodeJS |
| `gradle jsIrBuiltInFastBenchmark` | Executes benchmark for 'jsIrBuiltIn' with NodeJS |
| `gradle jsIrBuiltInForkBenchmark` | Executes benchmark for 'jsIrBuiltIn' with NodeJS |
| `gradle jsIrBuiltInParamsBenchmark` | Executes benchmark for 'jsIrBuiltIn' with NodeJS |
| `gradle jsIrCsvBenchmark` | Executes benchmark for 'jsIr' with NodeJS |
| `gradle jsIrFastBenchmark` | Executes benchmark for 'jsIr' with NodeJS |
| `gradle jsIrForkBenchmark` | Executes benchmark for 'jsIr' with NodeJS |
| `gradle jsIrParamsBenchmark` | Executes benchmark for 'jsIr' with NodeJS |
| `gradle jvmBenchmark` | Execute benchmark for 'jvm' |
| `gradle jvmBenchmarkBenchmark` | Execute benchmark for 'jvmBenchmark' |
| `gradle jvmBenchmarkBenchmarkCompile` | Compile JMH source files for 'jvmBenchmark' |
| `gradle jvmBenchmarkBenchmarkGenerate` | Generate JMH source files for 'jvmBenchmark' |
| `gradle jvmBenchmarkBenchmarkJar` | Build JAR for JMH compiled files for 'jvmBenchmark' |
| `gradle jvmBenchmarkCompile` | Compile JMH source files for 'jvm' |
| `gradle jvmBenchmarkCsvBenchmark` | Execute benchmark for 'jvmBenchmark' |
| `gradle jvmBenchmarkFastBenchmark` | Execute benchmark for 'jvmBenchmark' |
| `gradle jvmBenchmarkForkBenchmark` | Execute benchmark for 'jvmBenchmark' |
| `gradle jvmBenchmarkGenerate` | Generate JMH source files for 'jvm' |
| `gradle jvmBenchmarkJar` | Build JAR for JMH compiled files for 'jvm' |
| `gradle jvmBenchmarkParamsBenchmark` | Execute benchmark for 'jvmBenchmark' |
| `gradle jvmCsvBenchmark` | Execute benchmark for 'jvm' |
| `gradle jvmFastBenchmark` | Execute benchmark for 'jvm' |
| `gradle jvmForkBenchmark` | Execute benchmark for 'jvm' |
| `gradle jvmParamsBenchmark` | Execute benchmark for 'jvm' |
| `gradle linkNativeBenchmarkReleaseExecutableNative` | Compile Native benchmark source files for 'native' |
| `gradle nativeBenchmark` | Executes benchmark for 'native' |
| `gradle nativeBenchmarkGenerate` | Generate Native source files for 'native' |
| `gradle nativeCsvBenchmark` | Executes benchmark for 'native' |
| `gradle nativeFastBenchmark` | Executes benchmark for 'native' |
| `gradle nativeForkBenchmark` | Executes benchmark for 'native' |
| `gradle nativeParamsBenchmark` | Executes benchmark for 'native' |
| `gradle paramsBenchmark` | Execute all benchmarks in a project |
| `gradle wasmBenchmark` | Executes benchmark for 'wasm' with D8 |
| `gradle wasmBenchmarkGenerate` | Generate Wasm source files for 'wasm' |
| `gradle wasmCsvBenchmark` | Executes benchmark for 'wasm' with D8 |
| `gradle wasmFastBenchmark` | Executes benchmark for 'wasm' with D8 |
| `gradle wasmForkBenchmark` | Executes benchmark for 'wasm' with D8 |
| `gradle wasmParamsBenchmark` | Executes benchmark for 'wasm' with D8 |

## Want to learn more?

Feel free to engage in benchmarking discussions on the `#benchmarks` channel on [Kotlinlang Slack](https://kotlinlang.org/community/slack), explore the `kotlinx-benchmark` tagged questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/kotlinx-benchmark), or dive into the [kotlinx-benchmark Github Discussions](https://github.com/Kotlin/kotlinx-benchmark/discussions) for more insights and interactions.

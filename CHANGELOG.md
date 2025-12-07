# CHANGELOG

## 0.4.15

- Made WasmJS/D8 tasks configuration-cache friendly [#309](https://github.com/Kotlin/kotlinx-benchmark/pull/309)
- Made Gradle plugin isolation compatible [#325](https://github.com/Kotlin/kotlinx-benchmark/pull/325)
- Updated publish-plugin to published signed plugin artifacts [#295](https://github.com/Kotlin/kotlinx-benchmark/pull/295)
- Updated default JMH version to `1.37` [#312](https://github.com/Kotlin/kotlinx-benchmark/pull/312)
- Introduced notebooks for benchmarking results analysis, and added a note about them to the plugin output [#330](https://github.com/Kotlin/kotlinx-benchmark/pull/330), [#327](https://github.com/Kotlin/kotlinx-benchmark/pull/327)
- Multiple various build infrastructure and documentation changes.

## 0.4.14

- Fixed various issues specific to Kotlin/JS projects [#292](https://github.com/Kotlin/kotlinx-benchmark/pull/292)
- Added benchmark name validation for JVM [#304](https://github.com/Kotlin/kotlinx-benchmark/pull/304)
- Various build infrastructure updates

## 0.4.13

- Support Kotlin 2.0.0 and newer [#255](https://github.com/Kotlin/kotlinx-benchmark/pull/255)
- Add support for non-packed klib files in Native [#256](https://github.com/Kotlin/kotlinx-benchmark/pull/256)
- Fix Native benchmark compilations triggering compile tasks on Gradle sync [#252](https://github.com/Kotlin/kotlinx-benchmark/pull/252)
- Do not log KMP host messages at warning level [#105](https://github.com/Kotlin/kotlinx-benchmark/issues/105)

## 0.4.12

- Support debug build configuration for K/N [#189](https://github.com/Kotlin/kotlinx-benchmark/issues/189)
- Improve the warning message on incompatible host for a registered native target [#231](https://github.com/Kotlin/kotlinx-benchmark/pull/231)

## 0.4.11

- Take into consideration `jvmToolchain` when running benchmarks [#176](https://github.com/Kotlin/kotlinx-benchmark/issues/176)
- Hide unintentionally public API with an opt-in annotation [#211](https://github.com/Kotlin/kotlinx-benchmark/issues/211)
- Use locale-insensitive decimal and thousands separators in reports formatting
- Don't add `experimental-wasm-gc` flag for NodeJs >= 22 [#212](https://github.com/Kotlin/kotlinx-benchmark/issues/212)
- Allow having benchmark classes in the root package in non-JVM platforms [#215](https://github.com/Kotlin/kotlinx-benchmark/issues/215)
- Improve error messages for invalid use of annotations
- Use `Classpath` normalization for classpath inputs of source generator tasks

## 0.4.10

- Support Kotlin 1.9.21
- Validate values of benchmark configuration options [#124](https://github.com/Kotlin/kotlinx-benchmark/issues/124),
  [#125](https://github.com/Kotlin/kotlinx-benchmark/issues/125)
- Improve Kotlin/Native implementation of Blockhole [#114](https://github.com/Kotlin/kotlinx-benchmark/issues/114)
- Fix parsing of `@Measurement` annotation that misbehaved in non-JVM platforms
- Improve error messages when a target is not supported
- Support nodejs() environment for Kotlin/Wasm target

## 0.4.9

- Support Kotlin 1.9.0
- Support registering multiplatform source sets as benchmark targets
- Support all native targets in accordance with the official Kotlin strategy
- Align the default values of configuration options with JMH [#75](https://github.com/Kotlin/kotlinx-benchmark/issues/75)

## 0.4.8

- Drop legacy JS support
- Support building large JARs [#95](https://github.com/Kotlin/kotlinx-benchmark/issues/95)
- Support Kotlin 1.8.20
- Fix JVM and Native configuration cache warnings

## 0.4.7

- Support Kotlin 1.8.0

## 0.4.6

- Support Gradle 8.0
- Sign kotlinx-benchmark-plugin artifacts with the Signing Plugin
- Upgrade Kotlin version to 1.7.20
- Upgrade Gradle version to 7.4.2

## 0.4.5

- Remove redundant jmh-core dependency from plugin

## 0.4.4

- Require the minimum Kotlin version of 1.7.0

## 0.4.3

- Require the minimum Kotlin version of 1.6.20

## 0.4.2

- Support JS IR backend
- Support Gradle 7.0 and newer [#67](https://github.com/Kotlin/kotlinx-benchmark/issues/67)
- Make `mode` configuration parameter work with values considered valid in README.MD
- Support benchmark @Param values containing spaces [#62](https://github.com/Kotlin/kotlinx-benchmark/issues/62)

## 0.4.1

- Require the minimum Kotlin version of 1.6.0

## 0.4.0

- Require the minimum Kotlin version of 1.5.30
- Add support for other Apple Kotlin/Native targets
- Improve Kotlin/Native support [#24](https://github.com/Kotlin/kotlinx-benchmark/issues/24)
  - Benchmark each method in its own process, previously all methods where benchmarked in the same process
  - Introduce `nativeFork` advanced configuration option with the following values:
    - "perBenchmark" (default) – executes all iterations of a benchmark in the same process (one binary execution)
    - "perIteration" – executes each iteration of a benchmark in a separate process, measures in cold Kotlin/Native runtime environment
  - Introduce `nativeGCAfterIteration` advanced configuration option that when set to `true`, additionally collects garbage after each measuring iteration (default is `false`)
- Rename "forks" configuration option to "jvmForks" and provide an option to not override fork value defined in `@Fork`
- Fix a failure due to the strict DuplicatesStrategy [#39](https://github.com/Kotlin/kotlinx-benchmark/issues/39)

## 0.3.1

- Support report format selection: json(default), csv/scsv or text [#34](https://github.com/Kotlin/kotlinx-benchmark/issues/34)
- Fix Gradle configuration cache problems

## 0.3.0

- Require the minimum Kotlin version of 1.4.30
- Require the minimum Gradle version of 6.8
- Change runtime artifact id from `kotlinx.benchmark.runtime` to `kotlinx-benchmark-runtime`
- Publish runtime to Maven Central instead of Bintray [#33](https://github.com/Kotlin/kotlinx-benchmark/issues/33)
- Change plugin id from `kotlinx.benchmark` to `org.jetbrains.kotlinx.benchmark`
- Change plugin artifact id from `kotlinx.benchmark.gradle` to `kotlinx-benchmark-plugin`
- Publish plugin to Gradle Plugin Portal instead of Bintray

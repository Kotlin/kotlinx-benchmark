# CHANGELOG

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

# Kotlin-KTS Example

[![Open in GitHub Codespaces](https://github.com/codespaces/badge.svg)](https://codespaces.new/Kotlin/kotlinx-benchmark)

## Project Structure

Inside of this example, you'll see the following folders and files:

```
/
├── build.gradle.kts
└── main/
    └── src/
        └── KtsTestBenchmark.kt
```

## Tasks

All tasks can be run from the root of the project, from a terminal:

| Task Name | Action |
| --- | --- |
| `gradle assembleBenchmarks` | Generate and build all benchmarks in the project |
| `gradle benchmark` | Execute all benchmarks in the project |
| `gradle mainBenchmark` | Execute benchmark for 'benchmarks' |
| `gradle mainBenchmarkCompile` | Compile JMH source files for 'benchmarks' |
| `gradle mainBenchmarkGenerate` | Generate JMH source files for 'benchmarks' |
| `gradle mainBenchmarkJar` | Build JAR for JMH compiled files for 'benchmarks' |

## Want to learn more?

Feel free to engage in benchmarking discussions on the `#benchmarks` channel on [Kotlinlang Slack](https://kotlinlang.org/community/slack), explore the `kotlinx-benchmark` tagged questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/kotlinx-benchmark), or dive into the [kotlinx-benchmark Github Discussions](https://github.com/Kotlin/kotlinx-benchmark/discussions) for more insights and interactions.

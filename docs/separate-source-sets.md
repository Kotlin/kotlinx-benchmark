# Benchmarking with Gradle: Creating Separate Source Sets

Elevate your project's performance potential with organized, efficient, and isolated benchmarks. This guide will walk you through the process of creating separate source sets for benchmarks in your Kotlin project with Gradle.

## Table of Contents

1. [What is a Source Set?](#what-is-a-source-set)
2. [Why Have Separate Source Sets for Benchmarks?](#why-have-separate-source-sets-for-benchmarks)
3. [Step-by-step Setup Guide](#setup-guide)
   - [Kotlin JVM Project](#jvm-project)
   - [Kotlin Multiplatform Project](#multiplatform-project)
4. [Frequently Asked Questions](#frequently-asked-questions)
5. [Troubleshooting](#troubleshooting)

## What is a Source Set? <a name="what-is-a-source-set"></a>

Before we delve into the details, let's clarify what a source set is. In Gradle, a source set represents a group of source files that are compiled and executed together. By default, every Gradle project includes two source sets: `main` for your application code and `test` for your test code.

A source set defines the location of your source code, the names of compiled classes, and their placement. It also handles additional assets such as resources and configuration files.

## Why Have Separate Source Sets for Benchmarks? <a name="why-have-separate-source-sets-for-benchmarks"></a>

Having separate source sets for benchmarks offers several advantages:

1. **Organization**: It helps maintain a clean and organized project structure. Segregating benchmarks from the main code makes it easier to navigate and locate specific code segments.

2. **Isolation**: Separating benchmarks ensures that the benchmarking code does not interfere with your main code or test code. This isolation guarantees accurate measurements without unintentional side effects.

3. **Flexibility**: Creating a separate source set allows you to manage your benchmarking code independently. You can compile, test, and run benchmarks without impacting your main source code.

## Step-by-step Setup Guide <a name="setup-guide"></a>

Below are the step-by-step instructions to set up separate source sets for benchmarks in both Kotlin JVM and Multiplatform projects:

### Kotlin JVM Project <a name="jvm-project"></a>

Transform your Kotlin JVM project with separate benchmark source sets by following these simple steps:

1. **Define Source Set**:

   Begin by defining a new source set in your `build.gradle` file. We'll use `benchmarks` as the name for the source set.

   ```groovy
   sourceSets {
       benchmarks
   }
   ```

2. **Propagate Dependencies**:

   Next, propagate dependencies and output from the `main` source set to your `benchmarks` source set. This ensures the `benchmarks` source set has access to classes and resources from the `main` source set.

   ```groovy
   dependencies {
       benchmarksCompile sourceSets.main.output + sourceSets.main.runtimeClasspath
   }
   ```

   You can also add output and `compileClasspath` from `sourceSets.test` in the same way if you wish to reuse some of the test infrastructure.

3. **Register Benchmark Source Set**:

   Finally, register your benchmark source set. This informs the kotlinx-benchmark tool that benchmarks reside within this source set and need to be executed accordingly.

   ```groovy
   benchmark {
       targets {
           register("benchmarks") 
       }
   }
   ```

### Kotlin Multiplatform Project <a name="multiplatform-project"></a>

Set up your Kotlin Multiplatform project to accommodate separate benchmark source sets by following these steps:

1. **Define New Compilation**:

   Start by defining a new compilation in your target of choice (e.g. jvm, js, etc.) in your `build.gradle.kts` file. In this example, we're associating the new compilation 'benchmark' with the `main` compilation of the `jvm` target.

   ```kotlin
   kotlin {
       jvm {
           compilations.create('benchmark') { associateWith(compilations.main) }
       }
   }
   ```

2. **Register Benchmark Compilation**:

   Conclude by registering your benchmark compilation. This notifies the kotlinx-benchmark tool that benchmarks are located within this compilation and should be executed accordingly.

   ```kotlin
   benchmark {
       targets {
           register("jvmBenchmark")
       }
   }
   ```

   For more information on creating a custom compilation, you can refer to the [Kotlin documentation on creating a custom compilation](https://kotlinlang.org/docs/multiplatform-configure-compilations.html#create-a-custom-compilation).

## Frequently Asked Questions <a name="frequently-asked-questions"></a>

Here are some common questions about creating separate source sets for benchmarks:

**Q: Can I use the same benchmark source set for multiple targets?**
A: While it's possible, it's generally recommended to have separate source sets for different targets to avoid configuration conflicts and ensure more accurate benchmarks.

**Q: I'm encountering issues when running benchmarks from the IDE. What should I do?**
A: Ensure that the `src/benchmark/kotlin` directory is marked as "Sources Root" in your IDE. If you're still experiencing difficulties, refer to the discussions in [issue #112](https://github.com/Kotlin/kotlinx-benchmark/pull/112) for potential solutions.

**Q: Where can I ask additional questions?**
A: Feel free to post any questions or issues on the [kotlinx-benchmark GitHub page](https://github.com/Kotlin/kotlinx-benchmark). The community is always ready to assist you!
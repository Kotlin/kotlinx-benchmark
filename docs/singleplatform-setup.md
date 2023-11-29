# Step-by-Step Setup Guide for Single-Platform Benchmarking Project Using kotlinx-benchmark

This guide will walk you through the process of setting up a single-platform benchmarking project in both Kotlin and Java using the kotlinx-benchmark library.

# Table of Contents

1. [Prerequisites](#prerequisites)
2. [Kotlin Project Setup](#koltin-project-setup)
    - [Step 1: Create a New Java Project](#step-1-create-a-new-java-project)
    - [Step 2: Add the Benchmark and AllOpen Plugin](#step-2-add-the-benchmark-plugin-and-allopen-plugin)
    - [Step 3: Configure the Benchmark Plugin](#step-3-configure-the-benchmark-plugin)
    - [Step 4: Write Benchmarks](#step-4-write-benchmarks)
    - [Step 5: Run Benchmarks](#step-5-run-benchmarks)
3. [Java Project Setup](#java-project-setup)
    - [Step 1: Create a New Kotlin Project](#step-1-create-a-new-kotlin-project)
    - [Step 2: Add the Benchmark Plugin](#step-2-add-the-benchmark-plugin-1)
    - [Step 3: Configure the Benchmark Plugin](#step-3-configure-the-benchmark-plugin-1)
    - [Step 4: Write Benchmarks](#step-4-write-benchmarks-1)
    - [Step 5: Run Benchmarks](#step-5-run-benchmarks-1)
4. [Conclusion](#conclusion)

## Prerequisites

Ensure your development environment meets the following [requirements](compatibility.md):

- **Kotlin**: Version 1.8.20 or newer.
- **Gradle**: Version 8.0 or newer.

## Kotlin Project Setup

### Step 1: Create a New Java Project

#### IntelliJ IDEA

Click `File` > `New` > `Project`, select `Java`, specify your `Project Name` and `Project Location`, ensure the `Project SDK` is 8 or higher, and click `Finish`.

#### Gradle Command Line

Open your terminal, navigate to the directory where you want to create your new project, and run `gradle init --type java-application`.

### Step 2: Add the Benchmark and AllOpen Plugin

When benchmarking Kotlin/JVM code with Java Microbenchmark Harness (JMH), it is necessary to use the [allopen plugin](https://kotlinlang.org/docs/all-open-plugin.html). This plugin ensures that your benchmark classes and methods are `open`, which is a requirement for JMH. 

<details open>
<summary><strong>Kotlin DSL</strong></summary>

In your `build.gradle.kts` file, add the following:

```kotlin
plugins {
    kotlin("jvm") version "1.8.21"
    kotlin("plugin.allopen") version "1.8.21"
    id("org.jetbrains.kotlinx.benchmark") version "0.4.8"
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}
```
</details>

<details>
<summary><strong>Groovy DSL</strong></summary>

In your `build.gradle` file, add the following:

```groovy
plugins {
    id 'org.jetbrains.kotlin.jvm' version '1.8.21'
    id 'org.jetbrains.kotlin.plugin.allopen' version '1.8.21'
    id 'org.jetbrains.kotlinx.benchmark' version '0.4.8'
}

allOpen {
    annotation 'org.openjdk.jmh.annotations.State'
}
```
</details>

In Kotlin, classes and methods are `final` by default, which means they can't be overridden. However, JMH requires the ability to generate subclasses for benchmarking, which is why we need to use the allopen plugin. This configuration ensures that any class annotated with `@State` is treated as `open`, allowing JMH to work as expected.

You can alternatively mark your benchmark classes and methods `open` manually, but using the `allopen` plugin improves code maintainability.

### Step 3: Configure the Benchmark Plugin

In your `build.gradle` or `build.gradle.kts` file, add the following:

```groovy
benchmark {
    targets {
        register("jvm")
    }
}
```

### Step 4: Write Benchmarks

Create a new source file in your `main/src` directory and write your benchmarks. Here's an example:

```java
package test;

import org.openjdk.jmh.annotations.*;

@State(Scope.Benchmark)
@Fork(1)
public class SampleJavaBenchmark {
    @Param({"A", "B"})
    String stringValue;

    @Param({"1", "2"})
    int intValue;
    
    @Benchmark
    public String stringBuilder() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(10);
        stringBuilder.append(stringValue);
        stringBuilder.append(intValue);
        return stringBuilder.toString();
    }
}
```

### Step 5: Run Benchmarks

In the terminal, navigate to your project's root directory and run `./gradlew benchmark`.

## Java Project Setup

### Step 1: Create a New Kotlin Project

#### IntelliJ IDEA

Click `File` > `New` > `Project`, select `Kotlin`, specify your `Project Name` and `Project Location`, ensure the `Project SDK` is 8 or higher, and click `Finish`.

#### Gradle Command Line

Open your terminal, navigate to the directory where you want to create your new project, and run `gradle init --type kotlin-application`.

### Step 2: Add the Benchmark Plugin

<details open>
<summary><strong>Kotlin DSL</strong></summary>

In your `build.gradle.kts` file, add the following:

```kotlin
plugins {
    id 'java'
    id("org.jetbrains.kotlinx.benchmark") version "0.4.8"
}
```
</details>

<details>
<summary><strong>Groovy DSL</strong></summary>

In your `build.gradle` file, add the following:

```groovy
plugins {
    id 'java'
    id 'org.jetbrains.kotlinx.benchmark' version '0.4.8'
}
```
</details>

### Step 3: Configure the Benchmark Plugin

In your `build.gradle` or `build.gradle.kts` file, add the following:

```groovy
benchmark {
    targets {
        register("main")
    }
}
```

### Step 4: Write Benchmarks

Create a new source file in your `src/main/java` directory and write your benchmarks. Here's an example:

```kotlin
package test

import org.openjdk.jmh.annotations.*
import java.util.concurrent.*

@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 0)
@Measurement(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
class KtsTestBenchmark {
    private var data = 0.0

    @Setup
    fun setUp() {
        data = 3.0
    }

    @Benchmark
    fun sqrtBenchmark(): Double {
        return Math.sqrt(data)
    }

    @Benchmark
    fun cosBenchmark(): Double {
        return Math.cos(data)
    }
}
```

### Step 5: Run Benchmarks

In the terminal, navigate to your project's root directory and run `./gradlew benchmark`.

## Conclusion

Congratulations! You've set up a single-platform benchmarking project using `kotlinx-benchmark`. Now you can write your own benchmarks to test the performance of your Java or Kotlin code. Happy benchmarking!
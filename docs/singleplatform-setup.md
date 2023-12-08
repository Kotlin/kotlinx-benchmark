# Step-by-Step Setup Guide for Single-Platform Benchmarking Project Using kotlinx-benchmark

This guide will walk you through the process of setting up a single-platform benchmarking project in both Kotlin and Java using kotlinx-benchmark.

# Table of Contents

1. [Prerequisites](#prerequisites)
2. [Kotlin JVM Project Setup](#kotlin-jvm-project-setup)
    - [Step 1: Create a New Kotlin Project](#step-1-create-a-new-kotlin-project)
    - [Step 2: Add the Benchmark Plugin](#step-2-add-the-benchmark-plugin)
    - [Step 3: Configure the Benchmark Plugin](#step-3-configure-the-benchmark-plugin)
    - [Step 4: Enable Maven Central](#step-4-enable-maven-central)
    - [Step 5: Add the Runtime Dependency](#step-5-add-the-runtime-dependency)
    - [Step 6: Write Benchmarks](#step-6-write-benchmarks)
    - [Step 7: Run Benchmarks](#step-7-run-benchmarks)
3. [Java Project Setup](#java-project-setup)
    - [Step 1: Create a New Java Project](#step-1-create-a-new-java-project)
    - [Step 2: Add the Benchmark and AllOpen Plugin](#step-2-add-the-benchmark-plugin)
    - [Step 3: Configure the Benchmark Plugin](#step-3-configure-the-benchmark-plugin-1)
    - [Step 4: Enable Maven Central](#step-4-enable-maven-central-1)
    - [Step 5: Add the Runtime Dependency](#step-5-add-the-runtime-dependency-1)
    - [Step 6: Write Benchmarks](#step-6-write-benchmarks-1)
    - [Step 7: Run Benchmarks](#step-7-run-benchmarks-1)
4. [Conclusion](#conclusion)

## Prerequisites

Ensure your development environment meets the following [requirements](compatibility.md):

- **Kotlin**: Version 1.9.20 or newer.
- **Gradle**: Version 7.4 or newer.

## Kotlin JVM Project Setup

### Step 1: Create a New Kotlin Project

<details open>
<summary><strong>IntelliJ IDEA</strong></summary>

Click `File` > `New` > `Project`, select `Kotlin`, ensure the SDK version is 8 or higher, complete all relevant details, and then click `Create`.

</details>

<details>
<summary><strong>Gradle Command Line</strong></summary>

Open your terminal, navigate to the directory where you want to create your new project, and run `gradle init --type kotlin-application`.

</details>

### Step 2: Add the Benchmark and AllOpen Plugin

When benchmarking Kotlin/JVM code with Java Microbenchmark Harness (JMH), it is recommended to use the [allopen plugin](https://kotlinlang.org/docs/all-open-plugin.html). This plugin ensures that your benchmark classes and methods are `open`, which is a requirement for JMH. You can alternatively mark your benchmark classes and methods `open` manually.

<details open>
<summary><strong>Kotlin DSL</strong></summary>

In your `build.gradle.kts` file, add the following:

```kotlin
plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.allopen") version "1.9.21"
    id("org.jetbrains.kotlinx.benchmark") version "0.4.10"
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
    id 'org.jetbrains.kotlin.jvm' version '1.9.21'
    id 'org.jetbrains.kotlin.plugin.allopen' version '1.9.21'
    id 'org.jetbrains.kotlinx.benchmark' version '0.4.10'
}

allOpen {
    annotation 'org.openjdk.jmh.annotations.State'
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

### Step 4: Enable Maven Central

In your `build.gradle.kts` file, enable Maven Central for dependencies lookup:

```kotlin
repositories {
    mavenCentral()
}
```

### Step 5: Add the Runtime Dependency

Next, you will need to add the `kotlinx-benchmark-runtime` dependency to your project.

<details open>
<summary><strong>Kotlin DSL</strong></summary>

In your `build.gradle.kts` file, add the dependency inside the `dependencies` block:

```kotlin
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.10")
}
```

</details>

<details>
<summary><strong>Groovy DSL</strong></summary>

In your `build.gradle` file, add the dependency inside the `dependencies` block:

```groovy
dependencies {
    implementation 'org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.10'
}
```

</details>

### Step 6: Write Benchmarks

Create a new source file in your `src/main/kotlin` directory and write your benchmarks. Here's an example:

```kotlin
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

### Step 7: Run Benchmarks

In the terminal, navigate to your project's root directory and run `./gradlew benchmark`. 

## Java Project Setup

### Step 1: Create a New Java Project

<details open>
<summary><strong>IntelliJ IDEA</strong></summary>

Click `File` > `New` > `Project`, select `Java`, ensure the SDK version is 8 or higher, complete all relevant details, and then click `Create`.

</details>

<details>
<summary><strong>Gradle Command Line</strong></summary>

Open your terminal, navigate to the directory where you want to create your new project, and run `gradle init --type java-application`.

</details>

### Step 2: Add the Benchmark Plugin

<details open>
<summary><strong>Kotlin DSL</strong></summary>

In your `build.gradle.kts` file, add the following:

```kotlin
plugins {
    id 'java'
    id("org.jetbrains.kotlinx.benchmark") version "0.4.10"
}
```
</details>

<details>
<summary><strong>Groovy DSL</strong></summary>

In your `build.gradle` file, add the following:

```groovy
plugins {
    id 'java'
    id 'org.jetbrains.kotlinx.benchmark' version '0.4.10'
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

### Step 4: Enable Maven Central

In your `build.gradle.kts` file, enable Maven Central for dependencies lookup:

```kotlin
repositories {
    mavenCentral()
}
```

### Step 5: Add the Runtime Dependency

Next, you will need to add the `kotlinx-benchmark-runtime` dependency to your project.

<details open>
<summary><strong>Kotlin DSL</strong></summary>

In your `build.gradle.kts` file, add the dependency inside the `dependencies` block:

```kotlin
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.10")
}
```

</details>

<details>
<summary><strong>Groovy DSL</strong></summary>

In your `build.gradle` file, add the dependency inside the `dependencies` block:

```groovy
dependencies {
    implementation 'org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.10'
}
```

</details>

### Step 6: Write Benchmarks

Create a new source file in your `src/main/java` directory and write your benchmarks. Here's an example:

```java
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

### Step 7: Run Benchmarks

In the terminal, navigate to your project's root directory and run `./gradlew benchmark`.

## Conclusion

Congratulations on setting up your single-platform benchmarking project with `kotlinx-benchmark`! Consider exploring [Writing Benchmarks](writing-benchmarks.md) for a guide on crafting effective benchmarks, and [Configuration Options](configuration-options.md) for customizing your setup. Happy benchmarking!
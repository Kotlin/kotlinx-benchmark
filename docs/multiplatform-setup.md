# Step-by-Step Setup Guide for a Multiplatform Benchmarking Project Using kotlinx-benchmark

This guide will walk you through the process of setting up a multiplatform benchmarking project in Kotlin using kotlinx-benchmark.

# Table of Contents

1. [Prerequisites](#prerequisites)
2. [Kotlin/JS Project Setup](#kotlinjs-project-setup)
3. [Kotlin/Native Project Setup](#kotlinnative-project-setup)
4. [Kotlin/WASM Project Setup](#kotlinwasm-project-setup)
5. [Multiplatform Project Setup](#multiplatform-project-setup)
6. [Conclusion](#conclusion)

## Prerequisites

Ensure your development environment meets the following [requirements](compatibility.md):

- **Kotlin**: Version 1.8.20 or newer.
- **Gradle**: Version 8.0 or newer.

## Kotlin/JS Project Setup

### Step 1: Add the Benchmark Plugin

<details open>
<summary><strong>Kotlin DSL</strong></summary>

In your `build.gradle.kts` file, add the benchmarking plugin:

```kotlin
plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlinx.benchmark") version "0.4.8"
}
```
</details>

<details>
<summary><strong>Groovy DSL</strong></summary>

In your `build.gradle` file, add the benchmarking plugin:

```groovy
plugins {
    id 'org.jetbrains.kotlin.multiplatform'
    id 'org.jetbrains.kotlinx.benchmark' version '0.4.8'
}
```
</details>

### Step 2: Configure the Benchmark Plugin

The next step is to configure the benchmark plugin to know which targets to run the benchmarks against. In this case, we're specifying `js` as the target platform:

```groovy
benchmark {
    targets {
        register("js")
    }
}
```

### Step 3: Specify the Node.js Target and Optional Compiler

In Kotlin/JS, set the Node.js runtime as your target:

```kotlin
kotlin {
    js {
        nodejs()
    }   
}
```

Optionally you can specify a compiler such as the [IR compiler](https://kotlinlang.org/docs/js-ir-compiler.html) and configure the benchmarking targets:

```kotlin
kotlin {
    js('jsIr', IR) { 
        nodejs() 
    }
    js('jsIrBuiltIn', IR) { 
        nodejs() 
    }
}
```

In this configuration, `jsIr` and `jsIrBuiltIn` are both set up for Node.js and use the IR compiler. The `jsIr` target relies on an external benchmarking library (benchmark.js), whereas `jsIrBuiltIn` leverages the built-in Kotlin benchmarking plugin. Choosing one depends on your specific benchmarking requirements.

### Step 4: Add the Runtime Library

To run benchmarks, add the runtime library, `kotlinx-benchmark-runtime`, to the dependencies of your source set and enable Maven Central for dependencies lookup:

```kotlin
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.4")
            }
        }
    }
}

repositories {
    mavenCentral()
}
```

### Step 5: Write Benchmarks

Create a new source file in your `src/main/kotlin` directory and write your benchmarks. Here's an example:

```kotlin
package benchmark

import org.openjdk.jmh.annotations.*

@State(Scope.Benchmark)
open class JSBenchmark {
    private var data = 0.0

    @Setup
    fun setUp() {
        data = 3.0
    }

    @Benchmark
    fun sqrtBenchmark(): Double {
        return kotlin.math.sqrt(data)
    }
}
```

### Step 6: Run Benchmarks

In the terminal, navigate to your project's root directory and run `./gradlew benchmark`.

## Kotlin/Native Project Setup

### Step 1: Add the Benchmark Plugin

<details open>
<summary><strong>Kotlin DSL</strong></summary>

In your `build.gradle.kts` file, add the benchmarking plugin:

```kotlin
plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlinx.benchmark") version "0.4.8"
}
```
</details>

<details>
<summary><strong>Groovy DSL</strong></summary>

In your `build.gradle` file, add the benchmarking plugin:

```groovy
plugins {
    id 'org.jetbrains.kotlin.multiplatform'
    id 'org.jetbrains.kotlinx.benchmark' version '0.4.8'
}
```
</details>

### Step 2: Configure the Benchmark Plugin

The next step is to configure the benchmark plugin to know which targets to run the benchmarks against. In this case, we're specifying `native` as the target platform:

```groovy
benchmark {
    targets {
        register("native")
    }
}
```

### Step 3: Add the Runtime Library

To run benchmarks, add the runtime library, `kotlinx-benchmark-runtime`, to the dependencies of your source set and enable Maven Central for dependencies lookup:

```kotlin
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.4")
            }
        }
    }
}

repositories {
    mavenCentral()
}
```

### Step 4: Write Benchmarks

Create a new source file in your `src/nativeMain/kotlin` directory and write your benchmarks. Here's an example:

```kotlin
package benchmark

import org.openjdk.jmh.annotations.*

@State(Scope.Benchmark)
open class NativeBenchmark {
    private var data = 0.0

    @Setup
    fun setUp() {
        data = 3.0
    }

    @Benchmark
    fun sqrtBenchmark(): Double {
        return kotlin.math.sqrt(data)
    }
}
```

### Step 5: Run Benchmarks

In the terminal, navigate to your project's root directory and run `./gradlew benchmark`.

## Kotlin/WASM Project Setup

### Step 1: Add the Benchmark Plugin

<details open>
<summary><strong>Kotlin DSL</strong></summary>

In your `build.gradle.kts` file, add the following:

```kotlin
plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlinx.benchmark") version "0.4.8"
}
```
</details>

<details>
<summary><strong>Groovy DSL</strong></summary>

In your `build.gradle` file, add the following:

```groovy
plugins {
    id 'org.jetbrains.kotlin.multiplatform'
    id 'org.jetbrains.kotlinx.benchmark' version '0.4.8'
}
```
</details>

### Step 2: Configure the Benchmark Plugin

The next step is to configure the benchmark plugin to know which targets to run the benchmarks against. In this case, we're specifying `wasm` as the target platform:

```groovy
benchmark {
    targets {
        register("wasm")
    }
}
```

### Step 3: Add Runtime Library

To run benchmarks, add the runtime library, `kotlinx-benchmark-runtime`, to the dependencies of your source set and enable Maven Central for dependencies lookup:

```kotlin
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.4")
            }
        }
    }
}

repositories {
    mavenCentral()
}
```

### Step 4: Write Benchmarks

Create a new source file in your `src/wasmMain/kotlin` directory and write your benchmarks. Here's an example:

```kotlin
package benchmark

import org.openjdk.jmh.annotations.*

@State(Scope.Benchmark)
open class WASMBenchmark {
    private var data = 0.0

    @Setup
    fun setUp() {
        data = 3.0
    }

    @Benchmark
    fun sqrtBenchmark(): Double {
        return kotlin.math.sqrt(data)
    }
}
```

### Step 5: Run Benchmarks

In the terminal, navigate to your project's root directory and run `./gradlew benchmark`. For a practical example, please refer to [examples](../examples/multiplatform).

## Kotlin Multiplatform Project Setup

### Step 1: Add the Benchmark Plugin

<details open>
<summary><strong>Kotlin DSL</strong></summary>

In your `build.gradle.kts` file, add the following:

```kotlin
plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlinx.benchmark") version "0.4.8"
}
```
</details>

<details>
<summary><strong>Groovy DSL</strong></summary>

In your `build.gradle` file, add the following:

```groovy
plugins {
    id 'org.jetbrains.kotlin.multiplatform'
    id 'org.jetbrains.kotlinx.benchmark' version '0.4.8'
}
```
</details>

### Step 2: Configure the Benchmark Plugin

In your `build.gradle` or `build.gradle.kts` file, add the following:

```groovy
benchmark {
    targets {
        register("jvm")
        register("js")
        register("native")
        register("wasm")
    }
}
```

### Step 3: Add the Runtime Library

To run benchmarks, add the runtime library, `kotlinx-benchmark-runtime`, to the dependencies of your source set and enable Maven Central for dependencies lookup:

```kotlin
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.8")
            }
        }
    }
}

repositories {
    mavenCentral()
}
```

### Step 4: Write Benchmarks

Create new source files in your respective `src/<target>Main/kotlin` directories and write your benchmarks. 

### Step 5: Run Benchmarks

In the terminal, navigate to your project's root directory and run `./gradlew benchmark`.

## Conclusion

This guide has walked you through setting up a multiplatform benchmarking project using the kotlinx-benchmark library in Kotlin. It has covered the creation of new projects, the addition and configuration of the benchmark plugin, writing benchmark tests, and running these benchmarks. Remember, performance benchmarking is an essential part of optimizing your code and ensuring it runs as efficiently as possible. Happy benchmarking!
## Step-by-Step Guide for Multiplatform Benchmarking Setup Using kotlinx.benchmark

### Prerequisites

Before starting, ensure your development environment meets the following requirements:

- **Kotlin**: Version 1.8.20 or newer. Install Kotlin from the [official website](https://kotlinlang.org/) or a package manager like SDKMAN! or Homebrew.
- **Gradle**: Version 8.0 or newer. Download Gradle from the [official website](https://gradle.org/) or use a package manager.

### Step 1: Create a New Kotlin Multiplatform Project

Begin by creating a new Kotlin Multiplatform project. You can do this either manually or by using an IDE such as IntelliJ IDEA, which can generate the project structure for you.

### Step 2: Configure Build

In this step, you'll modify your project's build file to add necessary dependencies and plugins.

<details open>
<summary><strong>Kotlin DSL</strong></summary>

#### 2.1 Apply the Necessary Plugins

In your `build.gradle.kts` file, add the required plugins. These plugins are necessary for enabling Kotlin Multiplatform, making all classes and functions open, and using the kotlinx.benchmark plugin.

```kotlin
plugins {
    kotlin("multiplatform")
    kotlin("plugin.allopen") version "1.8.21"
    id("org.jetbrains.kotlinx.benchmark") version "0.4.8"
}
```

#### 2.2 Add the Dependencies

Next, add the `kotlinx-benchmark-runtime` dependency to your project. This dependency contains the necessary runtime components for benchmarking.

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
```

#### 2.3 Apply the AllOpen Annotation

Now, you need to instruct the [allopen](https://kotlinlang.org/docs/all-open-plugin.html) plugin to consider all benchmark classes and their methods as open. For that, apply the `allOpen` block and specify the JMH annotation `State`.

```kotlin
allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}
```

#### 2.4 Define the Repositories

Gradle needs to know where to find the libraries your project depends on. In this case, we're using the libraries hosted on Maven Central, so we need to specify that.

In your `build.gradle.kts` file, add the following code block:

```kotlin
repositories {
    mavenCentral()
}
```

#### 2.5 Register the Benchmark Targets

Next, we need to inform the kotlinx.benchmark plugin about our benchmarking targets. For multiplatform projects, we need to register each platform separately.

In your `build.gradle.kts` file, add the following code block within the `benchmark` block:

```kotlin
benchmark {
    targets {
        register("jvm")
        register("js")
        register("native")
        // Add more platforms as needed
    }
}
```

#### 2.6 Define the Kotlin Targets and SourceSets

In the `kotlin` block, you define the different platforms that your project targets and the related source sets. Within each target, you can specify the related compilations. For the JVM, you create a specific 'benchmark' compilation associated with the main compilation.

```kotlin
jvm {
    compilations.create('benchmark') { associateWith(compilations.main) }
}
```

The dependency `kotlinx-benchmark-runtime` is applied to the `commonMain` source set, indicating that it will be used across all platforms:

```kotlin
sourceSets {
    commonMain {
        dependencies {
            implementation project(":kotlinx-benchmark-runtime")
        }
    }
}
```

</details>

<details>
<summary><strong>Groovy DSL</strong></summary>

#### 2.1 Apply the Necessary Plugins

In your `build.gradle` file, apply the required plugins. These plugins are necessary for enabling Kotlin Multiplatform, making all classes and functions open, and using the kotlinx.benchmark plugin.

```groovy
plugins {
    id 'org.jetbrains.kotlin.multiplatform'
    id 'org.jetbrains.kotlin.plugin.allopen' version '1.8.21'
    id 'org.jetbrains.kotlinx.benchmark' version '0.4.8'
}
```

#### 2.2 Add the Dependencies

Next, add the `kotlinx-benchmark-runtime` dependency to your project. This dependency contains the necessary runtime components for benchmarking.

```groovy
dependencies {
    implementation 'org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.8'
}
```

#### 2.3 Apply the AllOpen Annotation

Now, you need to instruct the [allopen](https://kotlinlang.org/docs/all-open-plugin.html) plugin to consider all benchmark classes and their methods as open. For that, apply the `allOpen` block and specify the JMH annotation `State`.

```groovy
allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}
```

#### 2.4 Define the Repositories

Gradle needs to know where to find the libraries your project depends on. In this case, we're using the libraries hosted on Maven Central, so we need to specify that.

In your `build.gradle` file, add the following code block:

```groovy
repositories {
    mavenCentral()
}
```

#### 2.5 Register the Benchmark Targets

Next, we need to inform the kotlinx.benchmark plugin about our benchmarking targets. For multiplatform projects, we need to register each platform separately.

In your `build.gradle` file, add the following code block within the `benchmark` block:

```groovy
benchmark {
    targets {
        register("jvm")
        register("js")
        register("native")
        // Add more platforms as needed
    }
}
```

#### 2.6 Define the Kotlin Targets and SourceSets

In the `kotlin` block, you define the different platforms that your project targets and the related source sets. Within each target, you can specify the related compilations. For the JVM, you create a specific 'benchmark' compilation associated with the main compilation.

```kotlin
jvm {
    compilations.create('benchmark') { associateWith(compilations.main) }
}
```

The dependency `kotlinx-benchmark-runtime` is applied to the `commonMain` source set, indicating that it will be used across all platforms:

```kotlin
sourceSets {
    commonMain {
        dependencies {
            implementation project(":kotlinx-benchmark-runtime")
        }
    }
}
```

</details>

### Step 3: Writing Benchmarks

Create a new Kotlin source file in your `src/main/kotlin` directory to write your benchmarks. Each benchmark is a class or object with methods annotated with `@Benchmark`. Here's a simple example:

```kotlin
import org.openjdk.jmh.annotations.Benchmark

open class ListBenchmark {
    @Benchmark
    fun listOfBenchmark() {
        listOf(1, 2, 3, 4, 5)
    }
}
```

Ensure that your benchmark class and methods are `open`, as JMH creates subclasses during the benchmarking process. The `allopen` plugin we added earlier enforces this.

### Step 4: Running Your Benchmarks

Executing your benchmarks is an important part of the process. This will allow you to gather performance data about your code. There are two primary ways to run your benchmarks: through the command line or using your IDE.

#### 4.1 Running Benchmarks From the Command Line

The simplest way to run your benchmarks is by using the Gradle task `benchmark`. You can do this by opening a terminal, navigating to the root of your project, and entering the following command:

```bash
./gradlew benchmark
```

This command instructs Gradle to execute the `benchmark` task, which in turn runs your benchmarks.

#### 4.2 Understanding Benchmark Execution

The execution of your benchmarks might take some time. This is normal and necessary: benchmarks must be run for a sufficient length of time to produce reliable, statistically significant results.

For more details regarding the available Gradle tasks, refer to this [document](docs/tasks-overview.md).

### Step 5: Analyze the Results

To fully understand and make the best use of these results, it's important to know how to interpret and analyze them properly. For a comprehensive guide on interpreting and analyzing benchmarking results, please refer to this dedicated document: [Interpreting and Analyzing Results](docs/interpreting-results.md).

Congratulations! You have successfully set up a Kotlin Multiplatform benchmark project using kotlinx-benchmark.

[![JetBrains incubator project](https://jb.gg/badges/incubator.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)
[![Download](https://api.bintray.com/packages/kotlin/kotlinx/kotlinx.benchmark/images/download.svg) ](https://bintray.com/kotlin/kotlinx/kotlinx.benchmark/_latestVersion)

**kotlinx.benchmark** is a toolkit for running benchmarks for multiplatform code written in Kotlin 
and running on the next supported targets: JVM, JavaScript.

Technically it can be run on Native target, but current implementation doesn't allow to get right measurements in many 
cases for native benchmarks, so it isn't recommended to use this library for native benchmarks yet. 
See [issue](https://github.com/Kotlin/kotlinx-benchmark/issues/24) for more information.

If you're familiar with [JMH](https://openjdk.java.net/projects/code-tools/jmh/), it is very similar and uses it under 
the hoods to run benchmarks on JVM.   

# Requirements

Gradle 6.0 or newer

Kotlin 1.4.30 or newer

# Gradle plugin

Add repository in `settings.gradle` to enable bintray repository for plugin lookup

```groovy
pluginManagement {
    repositories {
        maven { url 'https://dl.bintray.com/kotlin/kotlinx' }
        gradlePluginPortal()
    }
}
```

Use plugin in `build.gradle`:

```groovy
plugins {
    id 'kotlinx.benchmark' version "0.2.0-dev-20"
}
```

Alternatively, you can use build script dependencies and "apply plugin" syntax:

```groovy
buildscript {
    repositories {
        …
        maven { url 'https://dl.bintray.com/kotlin/kotlinx' }
    }
    dependencies {
        classpath "org.jetbrains.kotlinx:kotlinx.benchmark.gradle:0.2.0-dev-20"
    }
}
 
apply plugin: 'kotlinx.benchmark'
```

For Kotlin/JS specify building `nodejs` flavour:

```groovy
kotlin {
    js {
        nodejs()
        …
    }   
}
```

For Kotlin/JVM code, add `allopen` plugin to make JMH happy. Alternatively, make all benchmark classes and methods `open`.

For example, if you annotated each of your benchmark classes with `@State(Scope.Benchmark)`:
```kotlin
@State(Scope.Benchmark)
class Benchmark {
    …
}
```
and added the following code to your `build.gradle`:
```groovy
plugins {
    id 'org.jetbrains.kotlin.plugin.allopen'
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}
```
then you don't have to make benchmark classes and methods `open`. 

# Runtime Library

You need a runtime library with annotations and code that will run benchmarks on JavaScript and Native platforms.

```groovy
repositories {
    maven { url 'https://dl.bintray.com/kotlin/kotlinx' }
}

kotlin {
    sourceSets {
        commonMain {
             dependencies {
                 implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.2.0-dev-20")
             }
        }
    }
}
```

To use the library in a JVM-only project add the platform to the artifact name, e.g.:

```groovy
implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime-jvm:0.2.0-dev-20")
```

# Configuration

In a `build.gradle` file create ` benchmark` section, and inside it add a `targets` section.
In this section register all targets you want to run benchmarks from. 
Example for multiplatform project:

```groovy
benchmark {
    targets {
        register("jvm") 
        register("js")
        register("native")
    }
}
```

This package can also be used for Java and Kotlin/JVM projects. Register a Java sourceSet as a target:

```groovy
benchmark {
    targets {
        register("main") 
    }
}
```

To configure benchmarks and create multiple profiles, create a `configurations` section in the `benchmark` block,
and place options inside. Toolkit creates `main` configuration by default, and you can create as many additional
configurations, as you need.   


```groovy
benchmark {
    configurations {
        main { 
            // configure default configuration
        }
        smoke { 
            // create and configure "smoke" configuration, e.g. with several fast benchmarks to quickly check
            // if code changes result in something very wrong, or very right. 
        }       
    }
}
```

Available configuration options:

* `iterations` – number of measuring iterations
* `warmups` – number of warm up iterations
* `iterationTime` – time to run each iteration (measuring and warmup)
* `iterationTimeUnit` – time unit for `iterationTime` (default is seconds)
* `outputTimeUnit` – time unit for results output
* `mode` – "thrpt" for measuring operations per time, or "avgt" for measuring time per operation
* `include("…")` – regular expression to include benchmarks with fully qualified names matching it, as a substring
* `exclude("…")` – regular expression to exclude benchmarks with fully qualified names matching it, as a substring
* `param("name", "value1", "value2")` – specify a parameter for a public mutable property `name` annotated with `@Param`
  
Time units can be NANOSECONDS, MICROSECONDS, MILLISECONDS, SECONDS, MINUTES, or their short variants such as "ms" or "ns".  
  
Example: 

```groovy
benchmark {
    // Create configurations
    configurations {
        main { // main configuration is created automatically, but you can change its defaults
            warmups = 20 // number of warmup iterations
            iterations = 10 // number of iterations
            iterationTime = 3 // time in seconds per iteration
        }
        smoke {
            warmups = 5 // number of warmup iterations
            iterations = 3 // number of iterations
            iterationTime = 500 // time in seconds per iteration
            iterationTimeUnit = "ms" // time unity for iterationTime, default is seconds
        }   
    }
    
    // Setup targets
    targets {
        // This one matches compilation base name, e.g. 'jvm', 'jvmTest', etc
        register("jvm") {
            jmhVersion = "1.21" // available only for JVM compilations & Java source sets
        }
        register("js") {
            // Note, that benchmarks.js uses a different approach of minTime & maxTime and run benchmarks
            // until results are stable. We estimate minTime as iterationTime and maxTime as iterationTime*iterations
        }
        register("native")
    }
}
```  
  
# Separate source sets for benchmarks

Often you want to have benchmarks in the same project, but separated from main code, much like tests. Here is how:

Define source set:
```groovy
sourceSets {
    benchmarks
}
```

Propagate dependencies and output from `main` sourceSet. 

```groovy
dependencies {
    benchmarksCompile sourceSets.main.output + sourceSets.main.runtimeClasspath 
}
```

You can also add output and compileClasspath from `sourceSets.test` in the same way if you want 
to reuse some of the test infrastructure.


Register `benchmarks` source set:

```groovy
benchmark {
    targets {
        register("benchmarks")    
    }
}
```

# Examples

The project contains [examples](https://github.com/Kotlin/kotlinx-benchmark/tree/master/examples) subproject that demonstrates using the library.
 

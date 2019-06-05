[![JetBrains incubator project](https://jb.gg/badges/incubator.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)

# Setting up

Add repository in `settings.gradle` to enable my bintray repository for plugin lookup

```groovy
pluginManagement {
    repositories {
        maven { url 'https://dl.bintray.com/orangy/maven' }
        gradlePluginPortal()
    }
}
```

If you are using it for Kotlin Multiplatform, enable metadata in `settings.gradle`:

```groovy
enableFeaturePreview('GRADLE_METADATA')
```

For Kotlin/JS code, add Node plugin as well:

```groovy
plugins {
    id 'kotlinx.team.node' 
}

node {
    version = "$node_version"
}
```

For Kotlin/JVM code, add `allopen` plugin to make JMH happy:

```groovy
plugins {
    id 'org.jetbrains.kotlin.plugin.allopen' version "1.3.30"
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}
```

# Adding multiplatform runtime library

For JVM benchmarks you don't need anything, JMH core is added automatically.
If you want to author multiplatform (especially common) benchmarks, you need a runtime library with small subset of 
annotations and code that will wire things up. The dependency is added automatically, but you need to add a repository
so that Gradle can resolve this dependency. 

```groovy
repositories {
    maven { url 'https://dl.bintray.com/orangy/maven' }
}
```

# Configuring benchmark targets

In a `build.gradle` file create ` benchmark` section, and inside it add a `targets` section.
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

Example for plain Java project:

```groovy
benchmark {
    targets {
        register("main") 
    }
}
```

Configure benchmarks:

```groovy
benchmark {
    // Create configurations
    configurations {
        main { // main configuration is created automatically, but you can change its defaults
            iterations = 10 // number of iterations
            iterationTime = 3 // time in seconds per iteration
        }
        fast {
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
benchmarks {
    targets {
        register("benchmarks")    
    }
}
```

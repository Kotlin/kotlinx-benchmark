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
    id 'org.jetbrains.kotlin.plugin.allopen' version "1.3.20"
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

> Please note that in this early project stage only macOS x64 binaries are published. 
If you need to run it on Linux or Windows, check out sources, update `build.gradle` for `runtime` project, and publish
to Maven Local repository.   

# Configuring benchmark source sets

In a `build.gradle` file create ` benchmark` section, and inside it add a `configurations` section.
Example for multiplatform project:

```groovy
benchmark {
    configurations {
        register("jvm") 
        register("js")
        register("native")
    }
}
```

Example for plain Java project:

```groovy
benchmark {
    configurations {
        register("main") 
    }
}
```

Configure benchmarks:

```groovy
benchmark {
    defaults { // specify defaults for all configurations
        iterations = 10 // number of iterations
        iterationTime = 1000 // time in ms per iteration
    }
    
    // Setup configurations
    configurations {
        // This one matches compilation base name, e.g. 'jvm', 'jvmTest', etc
        register("jvm") {
            jmhVersion = "1.21" // available only for JVM compilations & Java source sets
            
            // for now, we use iterations and iterationTime for both warmup and measurements
        }
        register("js") {
            // Note, that benchmarks.js uses a different approach of minTime & maxTime and run benchmarks
            // until results are stable. We estimate minTime as iterationTime and maxTime as iterationTime*iterations
        }
        register("native") {
            iterationTime = 2000 // override the default
        }
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
    benchmarksCompile sourceSets.main.output + sourceSets.main.compileClasspath 
}
```

You can also add output and compileClasspath from `sourceSets.test` in the same way if you want 
to reuse some of the test infrastructure.


Register `benchmarks` source set:

```groovy
benchmarks {
    configurations {
        register("benchmarks")    
    }
}
```

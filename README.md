# Setting up

Add repository in `settings.gradle` to enable my bintray repository for plugin lookup

```groovy
pluginManagement {
    repositories {
        maven { url 'https://dl.bintray.com/orangy/maven' }
        maven { url 'https://plugins.gradle.org/m2/' }
    }
}
```

If you are using it for Kotlin Multiplatform, enable metadata in `settings.gradle`:

```groovy
enableFeaturePreview('GRADLE_METADATA')
```

In `build.gradle` apply plugin:

```groovy
plugins {
    id 'org.jetbrains.gradle.benchmarks.plugin' version '0.1.5'
}
```

For Kotlin/JS code, add Node plugin as well:

```groovy
plugins {
    id 'com.moowork.node' version '1.2.0'
}

node {
    version = "$node_version"
    npmVersion = "$npm_version"
    download = true
    nodeModulesDir = file(buildDir)
}

// Workaround the problem with Node downloading
repositories.whenObjectAdded {
    if (it instanceof IvyArtifactRepository) {
        metadataSources {
            artifact()
        }
    }
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
annotations and code that will wire things up:

```groovy
repositories {
    maven { url 'https://dl.bintray.com/orangy/maven' }
}

sourceSets {
    commonMain {
        dependencies {
            implementation 'org.jetbrains.kotlin:kotlin-stdlib-common'
            implementation 'org.jetbrains.gradle.benchmarks:runtime:0.1.5'
        }
    }
    …
}
        
```

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
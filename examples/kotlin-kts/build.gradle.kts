@file:Suppress("UnstableApiUsage")

import kotlinx.benchmark.gradle.*
import org.jetbrains.kotlin.allopen.gradle.*
import org.jetbrains.kotlin.gradle.tasks.*

plugins {
    java
    kotlin("jvm") 
    kotlin("plugin.allopen") version "1.3.40-eap-104"
    id("kotlinx.benchmark") version "0.2.0" 
}

sourceSets.all {
    java.srcDir("$name/src")
    resources.srcDir("$name/resources")
}

configure<AllOpenExtension> {
    annotation("org.openjdk.jmh.annotations.State") 
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))    
    implementation(project(":kotlinx.benchmark.runtime"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

benchmark {
    targets {
        register("main") {
            this as JvmBenchmarkTarget
            jmhVersion = "1.21"
        }
    }
}
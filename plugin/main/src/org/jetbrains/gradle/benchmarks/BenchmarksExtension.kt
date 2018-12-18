package org.jetbrains.gradle.benchmarks

import groovy.lang.*
import org.gradle.api.*
import org.gradle.api.tasks.*

open class BenchmarksExtension(private val project: Project) {
    var buildDir: String = "benchmarks"

    val configurations: NamedDomainObjectContainer<BenchmarkConfiguration> =
        project.container(BenchmarkConfiguration::class.java) { name ->
            BenchmarkConfiguration(name)
        }

    fun configurations(configureClosure: Closure<NamedDomainObjectContainer<BenchmarkConfiguration>>) =
        configurations.configure(configureClosure)
}

open class BenchmarkConfiguration(val name: String) {
    var jmhVersion = "1.21"
}


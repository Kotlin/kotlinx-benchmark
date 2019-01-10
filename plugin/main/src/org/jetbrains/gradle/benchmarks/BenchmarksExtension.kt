package org.jetbrains.gradle.benchmarks

import groovy.lang.*
import org.gradle.api.*

open class BenchmarksExtension(private val project: Project) {
    var buildDir: String = "benchmarks"
    var reportsDir: String = "reports"

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


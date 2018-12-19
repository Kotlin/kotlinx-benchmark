package org.jetbrains.gradle.benchmarks

import groovy.lang.*
import org.gradle.api.*
import org.gradle.util.*
import java.io.*

fun cleanup(file: File) {
    if (file.exists()) {
        val listing = file.listFiles()
        if (listing != null) {
            for (sub in listing) {
                cleanup(sub)
            }
        }
        file.delete()
    }
}

val GRADLE_NEW = GradleVersion.current() >= GradleVersion.version("4.9-rc-1")

inline fun <reified T : Task> Project.task(
    name: String,
    depends: String? = null,
    noinline configuration: T.() -> Unit
) {
    when {
        GRADLE_NEW -> {
            @Suppress("UnstableApiUsage")
            val task = tasks.register(name, T::class.java, Action(configuration))
            if (depends != null) {
                tasks.getByName(depends).dependsOn(task)
            }
        }
        else -> {
            val task = tasks.create(name, T::class.java, Action(configuration))
            if (depends != null) {
                tasks.getByName(depends).dependsOn(task)
            }
        }
    }
}

fun Project.benchmarkBuildDir(extension: BenchmarksExtension, config: BenchmarkConfiguration): File {
    return file(buildDir.resolve(extension.buildDir).resolve(config.name))
}

class KotlinClosure1<in T : Any?, V : Any>(
    val function: T.() -> V?,
    owner: Any? = null,
    thisObject: Any? = null
) : Closure<V?>(owner, thisObject) {

    @Suppress("unused") // to be called dynamically by Groovy
    fun doCall(it: T): V? = it.function()
}

fun <T> Any.closureOf(action: T.() -> Unit): Closure<Any?> =
    KotlinClosure1(action, this, this)
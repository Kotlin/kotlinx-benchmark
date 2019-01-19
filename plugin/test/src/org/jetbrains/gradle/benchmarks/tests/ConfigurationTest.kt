package org.jetbrains.gradle.benchmarks.tests

import org.gradle.internal.classpath.*
import org.gradle.testkit.runner.*
import org.gradle.testkit.runner.TaskOutcome.*
import org.gradle.testkit.runner.internal.*
import org.junit.*
import org.junit.Test
import org.junit.rules.*
import java.io.*
import kotlin.test.*

class ConfigurationTest {
    @Rule
    @JvmField
    public val testProjectDir = TemporaryFolder()

    private lateinit var buildFile: File

    @Before
    fun setup() {
        buildFile = testProjectDir.newFile("build.gradle")
    }

    @Test
    fun javaProject() {
        val buildFileContent = buildString { 
            appendPlugins()
            appendBenchmarks()
        }

        buildFile.writeText(buildFileContent)

        println("======")
        println(buildFileContent)
        println("======")

        val result = gradleRunner(testProjectDir.root, "build", "--stacktrace").build()

        assertTrue(result.output.contains("Hello world!"))
        assertEquals(SUCCESS, result.task(":helloWorld")?.outcome)
    }

    fun gradleRunner(projectDir: File, vararg arguments: String) =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(arguments.toList())
            .withPluginClasspath()

    fun StringBuilder.appendBenchmarks() = append("""
benchmark {
    configurations {
        register("main") {}
    }
}
    """.trimIndent())
    
    fun StringBuilder.appendPlugins() = append(
        """
plugins {
    id 'java'
    id 'org.jetbrains.kotlin.jvm' 
    id 'org.jetbrains.kotlin.plugin.allopen' 
    id 'org.jetbrains.gradle.benchmarks.plugin' 
}

allOpen {
    annotation('org.openjdk.jmh.annotations.State')
}


    """.trimIndent()
    )
}
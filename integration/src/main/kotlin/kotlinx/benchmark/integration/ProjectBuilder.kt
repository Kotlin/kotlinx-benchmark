package kotlinx.benchmark.integration

import java.io.*

class ProjectBuilder {
    private val configurations = mutableMapOf<String, BenchmarkConfiguration>()

    fun configuration(name: String, configuration: BenchmarkConfiguration.() -> Unit = {}) {
        configurations[name] = BenchmarkConfiguration().apply(configuration)
    }

    fun build(original: String): String {

        val script =
            """
benchmark {
    configurations {
        ${configurations.flatMap { it.value.lines(it.key) }.joinToString("\n        ")}
    }
}
            """.trimIndent()

        return buildScript + "\n\n" + original + "\n\n" + script
    }
}

private val buildScript = run {
    """
    buildscript {
        dependencies {
            classpath files(${readFileList("plugin-classpath.txt")})
        }
    }
    
    apply plugin: 'kotlin-multiplatform'
    apply plugin: 'org.jetbrains.kotlinx.benchmark'
    
    repositories {
        mavenCentral()
    }
    
    def benchmarkRuntimeMetadata = files(${readFileList("runtime-metadata.txt")})
    def benchmarkRuntimeJvm = files(${readFileList("runtime-jvm.txt")})
    def benchmarkRuntimeJs = files(${readFileList("runtime-js.txt")})
    def benchmarkRuntimeNative = files(${readFileList("runtime-native.txt")})
    def benchmarkRuntimeNativeMetadata = files(${readFileList("runtime-native-metadata.txt")})
    """.trimIndent()
}

private fun readFileList(fileName: String): String {
    val resource = ProjectBuilder::class.java.classLoader.getResource(fileName)
        ?: throw IllegalStateException("Could not find resource '$fileName'")
    val files = File(resource.toURI())
        .readLines()
        .map { File(it).absolutePath.replace("\\", "\\\\") } // escape backslashes in Windows paths
    return files.joinToString(", ") { "'$it'" }
}

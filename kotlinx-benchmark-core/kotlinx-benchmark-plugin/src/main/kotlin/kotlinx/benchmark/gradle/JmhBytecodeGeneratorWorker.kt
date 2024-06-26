/*
 * Copyright 2003-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.generators.core.*
import org.openjdk.jmh.generators.reflection.*
import org.openjdk.jmh.util.*
import java.io.*
import java.net.*
import java.util.*

@KotlinxBenchmarkPluginInternalApi
abstract class JmhBytecodeGeneratorWorker : WorkAction<JmhBytecodeGeneratorWorkParameters> {

    @KotlinxBenchmarkPluginInternalApi
    companion object {
        private const val classSuffix = ".class"
    }

    private val outputSourceDirectory: File get() = parameters.outputSourceDirectory.get().asFile
    private val outputResourceDirectory: File get() = parameters.outputResourceDirectory.get().asFile

    override fun execute() {
        cleanup(outputSourceDirectory)
        cleanup(outputResourceDirectory)

        val urls = (parameters.inputClasses + parameters.inputClasspath).map { it.toURI().toURL() }.toTypedArray()

        // Include compiled bytecode on classpath, in case we need to
        // resolve the cross-class dependencies
        val benchmarkAnnotation = Benchmark::class.java

        val currentThread = Thread.currentThread()
        val originalClassLoader = currentThread.contextClassLoader

        // TODO: This is some magic I don't understand yet
        // Somehow Benchmark class is loaded into a Launcher/App class loader and not current context class loader
        // Hence, if parent classloader is set to originalClassLoader then Benchmark annotation check doesn't work
        // inside JMH bytecode gen. This hack seem to work, but we need to understand
        val introspectionClassLoader = URLClassLoader(urls, benchmarkAnnotation.classLoader)

/*
        println("Original_Parent_ParentCL: ${originalClassLoader.parent.parent}")
        println("Original_ParentCL: ${originalClassLoader.parent}")
        println("OriginalCL: $originalClassLoader")
        println("IntrospectCL: $introspectionClassLoader")
        println("BenchmarkCL: ${benchmarkAnnotation.classLoader}")
*/

        try {
            currentThread.contextClassLoader = introspectionClassLoader
            generateJMH(urls, introspectionClassLoader)
        } finally {
            currentThread.contextClassLoader = originalClassLoader
        }
    }

    private fun generateJMH(urls: Array<URL>, introspectionClassLoader: URLClassLoader) {
        val destination = FileSystemDestination(outputResourceDirectory, outputSourceDirectory)

        val allFiles = HashMap<File, Collection<File>>(urls.size)
        for (directory in parameters.inputClasses) {
            val classes = FileUtils.getClasses(directory)
            allFiles[directory] = classes
        }

        val source = RFGeneratorSource()
        for ((directory, files) in allFiles) {
            println("Analyzing ${files.size} files from $directory")
            val directoryPath = directory.absolutePath
            for (file in files) {
                val resourceName = file.absolutePath.substring(directoryPath.length + 1)
                if (resourceName.endsWith(classSuffix)) {
                    val className = resourceName.replace('\\', '.').replace('/', '.')
                    val clazz = Class.forName(className.removeSuffix(classSuffix), false, introspectionClassLoader)
                    source.processClasses(clazz)
                }
            }
        }

        println("Writing out Java source to $outputSourceDirectory and resources to $outputResourceDirectory")
        val gen = BenchmarkGenerator()
        gen.generate(source, destination)
        gen.complete(source, destination)

        if (destination.hasErrors()) {
            var errCount = 0
            val sb = StringBuilder()
            for (e in destination.errors) {
                errCount++
                sb.append("  - ").append(e.toString()).append("\n")
            }
            throw RuntimeException("Generation of JMH bytecode failed with " + errCount + " errors:\n" + sb)
        }
    }
}

@KotlinxBenchmarkPluginInternalApi
interface JmhBytecodeGeneratorWorkParameters : WorkParameters {
    val inputClasses: ConfigurableFileCollection
    val inputClasspath: ConfigurableFileCollection
    val outputSourceDirectory: DirectoryProperty
    val outputResourceDirectory: DirectoryProperty
}

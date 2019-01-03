/*
 * Copyright 2003-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.gradle.benchmarks

import org.openjdk.jmh.generators.core.*
import org.openjdk.jmh.generators.reflection.*
import org.openjdk.jmh.util.*
import java.io.*
import java.net.*
import java.util.*
import javax.inject.*

class JmhBytecodeGeneratorWorker
@Inject constructor(
    private val compiledBytecodeDirectories: Set<File>,
    private val outputSourceDirectory: File,
    private val outputResourceDirectory: File
) : Runnable {

    companion object {
        private const val classSuffix = ".class"
    }

    override fun run() {
        cleanup(outputSourceDirectory)
        cleanup(outputResourceDirectory)

        val urls = compiledBytecodeDirectories.map { it.toURI().toURL() }.toTypedArray()

        // Include compiled bytecode on classpath, in case we need to
        // resolve the cross-class dependencies
        val introspectionClassLoader = URLClassLoader(urls, javaClass.classLoader)

        val currentThread = Thread.currentThread()
        val originalClassLoader = currentThread.contextClassLoader
        try {
            currentThread.contextClassLoader = introspectionClassLoader

            val destination = FileSystemDestination(outputResourceDirectory, outputSourceDirectory)

            val allFiles = HashMap<File, Collection<File>>(urls.size)
            for (directory in compiledBytecodeDirectories) {
                val classes = FileUtils.getClasses(directory)
                allFiles[directory] = classes
            }
            println("Writing out Java source to $outputSourceDirectory and resources to $outputResourceDirectory")

            for ((directory, files) in allFiles) {
                println("Processing " + files.size + " classes from " + directory)
                val source = RFGeneratorSource()
                val directoryPath = directory.absolutePath
                for (file in files) {
                    val resourceName = file.absolutePath.substring(directoryPath.length + 1)
                    if (resourceName.endsWith(classSuffix)) {
                        val className = resourceName.replace('\\', '.').replace('/', '.')
                        val clazz = Class.forName(className.removeSuffix(classSuffix), false, introspectionClassLoader)
                        source.processClasses(clazz)
                    }
                }

                val gen = BenchmarkGenerator()
                gen.generate(source, destination)
                gen.complete(source, destination)
            }


            if (destination.hasErrors()) {
                var errCount = 0
                val sb = StringBuilder()
                for (e in destination.errors) {
                    errCount++
                    sb.append("  - ").append(e.toString()).append("\n")
                }
                throw RuntimeException("Generation of JMH bytecode failed with " + errCount + "errors:\n" + sb)
            }
        } finally {
            currentThread.contextClassLoader = originalClassLoader
        }
    }
}

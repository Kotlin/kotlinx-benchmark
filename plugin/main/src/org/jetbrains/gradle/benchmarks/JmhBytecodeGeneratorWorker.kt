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

import org.openjdk.jmh.generators.asm.*
import org.openjdk.jmh.generators.bytecode.*
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
    private val outputResourceDirectory: File,
    private val generatorType: String
) : Runnable {

    override fun run() {
        cleanup(outputSourceDirectory)
        cleanup(outputResourceDirectory)

        val urls = compiledBytecodeDirectories.map { it.toURI().toURL() }.toTypedArray()

        // Include compiled bytecode on classpath, in case we need to
        // resolve the cross-class dependencies
        val amendedCL = URLClassLoader(urls, this.javaClass.classLoader)

        val currentThread = Thread.currentThread()
        val ocl = currentThread.contextClassLoader
        try {
            currentThread.contextClassLoader = amendedCL

            val destination = FileSystemDestination(outputResourceDirectory, outputSourceDirectory)

            val allClasses = HashMap<File, Collection<File>>(urls.size)
            for (compiledBytecodeDirectory in compiledBytecodeDirectories) {
                val classes = FileUtils.getClasses(compiledBytecodeDirectory)
                println("Processing " + classes.size + " classes from " + compiledBytecodeDirectory + " with \"" + generatorType + "\" generator")
                allClasses[compiledBytecodeDirectory] = classes
            }
            println("Writing out Java source to $outputSourceDirectory and resources to $outputResourceDirectory")

            for ((compiledBytecodeDirectory, classes) in allClasses) {
                val source =
                    when {
                        generatorType.equals(JmhBytecodeGenerator.GENERATOR_TYPE_ASM, ignoreCase = true) -> {
                            ASMGeneratorSource().apply {
                                processClasses(classes)
                            }
                        }
                        else -> {
                            val src = RFGeneratorSource()
                            for (f in classes) {
                                var name = f.absolutePath.substring(compiledBytecodeDirectory.absolutePath.length + 1)
                                name = name.replace("\\\\".toRegex(), ".")
                                name = name.replace("/".toRegex(), ".")
                                if (name.endsWith(".class")) {
                                    val clazz = Class.forName(name.substring(0, name.length - 6), false, amendedCL)
                                    src.processClasses(clazz)
                                }
                            }
                            src
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
            currentThread.contextClassLoader = ocl
        }
    }
}

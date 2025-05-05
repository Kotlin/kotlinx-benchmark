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
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logging
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.generators.core.BenchmarkGenerator
import org.openjdk.jmh.generators.core.FileSystemDestination
import org.openjdk.jmh.generators.reflection.RFGeneratorSource
import org.openjdk.jmh.util.FileUtils
import java.io.File
import java.lang.reflect.Method
import java.net.URL
import java.net.URLClassLoader
import kotlin.reflect.jvm.kotlinFunction

@KotlinxBenchmarkPluginInternalApi
// TODO https://github.com/Kotlin/kotlinx-benchmark/issues/211
//      Change visibility of JmhBytecodeGeneratorWorker `internal`
//      Move to package kotlinx.benchmark.gradle.internal.generator.workers, alongside the other workers.
abstract class JmhBytecodeGeneratorWorker : WorkAction<JmhBytecodeGeneratorWorkParameters> {

    // TODO in version 1.0 replace JmhBytecodeGeneratorWorkParameters with this interface:
    //internal interface Parameters : WorkParameters {
    //    val inputClasses: ConfigurableFileCollection
    //    val inputClasspath: ConfigurableFileCollection
    //    val outputSourceDirectory: DirectoryProperty
    //    val outputResourceDirectory: DirectoryProperty
    //}

    internal companion object {
        private const val classSuffix = ".class"
        private val logger = Logging.getLogger(JmhBytecodeGeneratorWorker::class.java)
    }

    private val outputSourceDirectory: File get() = parameters.outputSourceDirectory.get().asFile
    private val outputResourceDirectory: File get() = parameters.outputResourceDirectory.get().asFile

    override fun execute() {
        outputSourceDirectory.deleteRecursively()
        outputResourceDirectory.deleteRecursively()

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
        var noValidationErrors = true
        for ((directory, files) in allFiles) {
            println("Analyzing ${files.size} files from $directory")
            val directoryPath = directory.absolutePath
            for (file in files) {
                val resourceName = file.absolutePath.substring(directoryPath.length + 1)
                if (resourceName.endsWith(classSuffix)) {
                    val className = resourceName.replace('\\', '.').replace('/', '.')
                    val clazz = Class.forName(className.removeSuffix(classSuffix), false, introspectionClassLoader)
                    source.processClasses(clazz)
                    noValidationErrors = noValidationErrors.and(validateBenchmarkFunctions(clazz))
                }
            }
        }

        check(noValidationErrors) {
            "One or more benchmark functions are invalid and could not be processed by JMH. See logs for details."
        }

        logger.lifecycle("Writing out Java source to $outputSourceDirectory and resources to $outputResourceDirectory")
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
            throw RuntimeException("Generation of JMH bytecode failed with $errCount errors:\n$sb")
        }
    }

    /**
     * Validates functions annotated with `@Benchmark` and return `true` if no issues were found.
     * Otherwise, the function returns `false` and logs all detected errors.
     */
    private fun validateBenchmarkFunctions(clazz: Class<*>): Boolean {
        // Using declaredMethods to abstain from reporting the same method multiple times in the case
        // of benchmark classes extending some other classes.
        return clazz.declaredMethods.filter { it.isAnnotationPresent(Benchmark::class.java) }
            .map(::validateBenchmarkFunction)
            .fold(true, Boolean::and)
    }

    /**
     * Validates a benchmark function [function] and return `true` if no issues were found.
     * Otherwise, the function returns `false` and logs all detected errors.
     */
    private fun validateBenchmarkFunction(function: Method): Boolean {
        isValidJavaFunctionName(function.name)?.let {
            logger.error(formatInvalidFunctionNameMessage(it, function))
            return false
        }
        return true
    }

    /**
     * Validates if [identifier] is a valid Java function name and return a string describing an error if it's not.
     * If the [identifier] is a valid function name, the function returns `null`.
     */
    private fun isValidJavaFunctionName(identifier: String): String? {
        // See Java Language Specification, §3.8 Identifiers
        if (reservedLiterals.contains(identifier)) {
            return "Benchmark function name is a boolean or null literal and cannot be used as a function name"
        }
        // See Java Language Specification, §3.9 Keywords
        if (reservedJavaIdentifierNames.contains(identifier)) {
            return "Benchmark function name is a reserved Java keyword and cannot be used"
        }
        // See Java Language Specification, §3.8 Identifiers
        if (!(Character.isJavaIdentifierStart(identifier.first())
                    && identifier.substring(1).all(Character::isJavaIdentifierPart))) {
            return "Benchmark function name is not a valid Java identifier"
        }

        return null
    }

    private fun formatInvalidFunctionNameMessage(errorDescription: String, function: Method): String {
        val holder = function.declaringClass
        val javaName = "${holder.canonicalName}.${function.name}"
        val kotlinName =
            "${holder.kotlin.qualifiedName ?: holder.name}.${function.kotlinFunction?.name ?: function.name}"
        return "$errorDescription: \"${javaName}\" (declared as \"$kotlinName\"). " +
                "This might happen if the function has a backticked (`) name, " +
                "illegal named specified in @JvmName annotation, or a function returns an inline value class. " +
                "Consider using @JvmName annotation to provide a valid runtime name."
    }
}

/**
 * Words reserved for boolean and null-literals, that could not be used as Java identifier names.
 *
 * See Java Language specification, §3.8. Identifiers for details.
 */
private val reservedLiterals = setOf("true", "false", "null")

/**
 * Reserved keywords that could not be used as Java identifier names.
 *
 * See Java Language specification, §3.9. Keywords for details.
 *
 * Note some keywords (like module or exports) are contextual and require some specific conditions
 * to be met before they will be recognized as keywords and become a problem for us.
 * Thus, they are not included here.
 */
private val reservedJavaIdentifierNames = setOf(
    "abstract", "continue", "for", "new", "switch",
    "assert", "default", "if", "package", "synchronized",
    "boolean", "do", "goto", "private", "this",
    "break", "double", "implements", "protected", "throw", "byte", "else", "import", "public", "throws",
    "case", "enum", "instanceof", "return", "transient",
    "catch", "extends", "int", "short", "try",
    "char", "final", "interface", "static", "void",
    "class", "finally", "long", "strictfp", "volatile",
    "const", "float", "native", "super", "while", "_"
)

@KotlinxBenchmarkPluginInternalApi
// TODO https://github.com/Kotlin/kotlinx-benchmark/issues/211
//      Move to a nested interface inside of JmhBytecodeGeneratorWorker (like the other workers)
interface JmhBytecodeGeneratorWorkParameters : WorkParameters {
    val inputClasses: ConfigurableFileCollection
    val inputClasspath: ConfigurableFileCollection
    val outputSourceDirectory: DirectoryProperty
    val outputResourceDirectory: DirectoryProperty
}

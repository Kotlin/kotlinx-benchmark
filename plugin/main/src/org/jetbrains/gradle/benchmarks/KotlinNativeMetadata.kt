/*
package org.jetbrains.gradle.benchmarks

import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.storage.*
import shadow.org.jetbrains.kotlin.konan.util.*
import shadow.org.jetbrains.kotlin.konan.library.KLIB_FILE_EXTENSION_WITH_DOT
import java.io.*

val defaultRepository = File(DependencyProcessor.localKonanDir.resolve("klib").absolutePath)

val currentLanguageVersion = LanguageVersion.LATEST_STABLE
val currentApiVersion = ApiVersion.LATEST_STABLE

fun libraryInRepo(repository: File, name: String) =
    resolverByName(listOf(repository.absolutePath), skipCurrentDir = true).resolve(name)

fun libraryInCurrentDir(name: String) = resolverByName(emptyList()).resolve(name)

fun libraryInRepoOrCurrentDir(repository: File, name: String) =
    resolverByName(listOf(repository.absolutePath)).resolve(name)

class Library(val name: String, val requestedRepository: String?, val target: String) {

    val repository = requestedRepository?.File() ?: defaultRepository
    fun info() {
        val library = libraryInRepoOrCurrentDir(repository, name)
        val headerAbiVersion = library.versions.abiVersion
        val headerCompilerVersion = library.versions.compilerVersion
        val headerLibraryVersion = library.versions.libraryVersion
        val moduleName = ModuleDeserializer(library.moduleHeaderData).moduleName

        println("")
        println("Resolved to: ${library.libraryName.File().absolutePath}")
        println("Module name: $moduleName")
        println("ABI version: $headerAbiVersion")
        println("Compiler version: ${headerCompilerVersion?.toString(true, true)}")
        println("Library version: $headerLibraryVersion")
        val targets = library.targetList.joinToString(", ")
        print("Available targets: $targets\n")
    }

    fun install() {
        if (!repository.exists) {
            warn("Repository does not exist: $repository. Creating.")
            repository.mkdirs()
        }

        Library(File(name).name.removeSuffix(KLIB_FILE_EXTENSION_WITH_DOT), requestedRepository, target).remove(true)

        val library = libraryInCurrentDir(name)
        val newLibDir = File(repository, library.libraryFile.name.removeSuffix(KLIB_FILE_EXTENSION_WITH_DOT))

        library.libraryFile.unpackZippedKonanLibraryTo(newLibDir)
    }

    fun remove(blind: Boolean = false) {
        if (!repository.exists) error("Repository does not exist: $repository")

        val library = try {
            val library = libraryInRepo(repository, name)
            if (blind) warn("Removing The previously installed $name from $repository.")
            library

        } catch (e: Throwable) {
            if (!blind) println(e.message)
            null

        }
        library?.libraryFile?.deleteRecursively()
    }

    fun contents(output: Appendable = System.out) {

        val storageManager = LockBasedStorageManager()
        val library = libraryInRepoOrCurrentDir(repository, name)
        val versionSpec = LanguageVersionSettingsImpl(currentLanguageVersion, currentApiVersion)
        val module = DefaultDeserializedDescriptorFactory.createDescriptorAndNewBuiltIns(library, versionSpec, storageManager)

        val defaultModules = mutableListOf<ModuleDescriptorImpl>()
        if (!module.isKonanStdlib()) {
            val resolver = resolverByName(
                emptyList(),
                distributionKlib = Distribution().klib,
                skipCurrentDir = true)
            resolver.defaultLinks(false, true)
                .mapTo(defaultModules) {
                    DefaultDeserializedDescriptorFactory.createDescriptor(
                        it, versionSpec, storageManager, module.builtIns)
                }
        }

        (defaultModules + module).let { allModules ->
            allModules.forEach { it.setDependencies(allModules) }
        }

        KlibPrinter(output).print(module)
    }
}
*/

package kotlinx.benchmark.gradle

import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import javax.inject.*

/**
 * Helper task for unpacking the Android AAR (or plain JAR) file and extracting classes
 * for further processing.
 */
internal abstract class AndroidUnpackLibraryArtifactTask @Inject constructor(
    private val fs: FileSystemOperations,
    private val archives: ArchiveOperations
) : DefaultTask() {

    // AAR or JAR file to be unpacked
    @get:InputFile
    abstract val libraryFile: RegularFileProperty

    // Directory in which the library will be unpacked
    @get:OutputDirectory
    abstract val unpackedAarDir: DirectoryProperty

    @TaskAction
    fun unpack() {
        val inputFile = libraryFile.get().asFile
        val dir = unpackedAarDir.get().asFile

        when (inputFile.extension.lowercase()) {
            "aar" -> {
                fs.sync { copySpec ->
                    with(copySpec) {
                        from(archives.zipTree(inputFile))
                        into(dir)
                    }
                }
            }
            "jar" -> {
                fs.copy { copySpec ->
                    with(copySpec) {
                        from(inputFile)
                        into(dir)
                        rename { "classes.jar" }
                    }
                }
            }
            else -> throw GradleException("Unsupported library file format: $inputFile")
        }

        val classesJar = dir.resolve("classes.jar")
        if (classesJar.exists()) {
            fs.copy { copySpec ->
                with(copySpec) {
                    from(archives.zipTree(classesJar))
                    into(dir.resolve("classes"))
                }
            }
        }
    }
}
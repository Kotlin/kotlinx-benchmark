package kotlinx.benchmark.klib

import kotlinx.metadata.klib.KlibModuleMetadata
import org.jetbrains.kotlin.library.components.metadata
import org.jetbrains.kotlin.library.loader.KlibLoader
import java.io.File

class KlibMetadataLoaderImpl : KlibMetadataLoader {
    override fun load(klibFile: File): KlibModuleMetadata {
        val libs = KlibLoader {
            libraryPaths(klibFile.absolutePath)
        }.load()

        check (!libs.hasProblems) {
            "Failed to load following libraries: ${libs.problematicLibraries.joinToString("; ") { "${it.libraryPath}: ${it.problemCase}" }}"
        }

        val library = libs.librariesStdlibFirst.first()
        return KlibModuleMetadata.read(object : KlibModuleMetadata.MetadataLibraryProvider {
            private val metadata = library.metadata
            override val moduleHeaderData: ByteArray
                get() = metadata.moduleHeaderData

            override fun packageMetadata(fqName: String, partName: String): ByteArray =
                metadata.getPackageFragment(fqName, partName)

            override fun packageMetadataParts(fqName: String): Set<String> =
                metadata.getPackageFragmentNames(fqName)
        })
    }
}

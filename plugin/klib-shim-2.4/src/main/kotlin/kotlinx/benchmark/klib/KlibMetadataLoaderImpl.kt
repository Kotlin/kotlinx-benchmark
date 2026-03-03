package kotlinx.benchmark.klib

import kotlinx.metadata.klib.KlibModuleMetadata
import org.jetbrains.kotlin.library.loader.KlibLoader
import java.io.File

class KlibMetadataLoaderImpl : KlibMetadataLoader {
    override fun load(klibFile: File): KlibModuleMetadata {
        val libs = KlibLoader {
            libraryPaths(klibFile.absolutePath)
        }.load()
        val library = libs.librariesStdlibFirst.first { it.libraryFile == klibFile }
        return KlibModuleMetadata.read(object : KlibModuleMetadata.MetadataLibraryProvider {
            override val moduleHeaderData: ByteArray
                get() = library.moduleHeaderData

            override fun packageMetadata(fqName: String, partName: String): ByteArray =
                library.packageMetadata(fqName, partName)

            override fun packageMetadataParts(fqName: String): Set<String> =
                library.packageMetadataParts(fqName)
        })
    }
}

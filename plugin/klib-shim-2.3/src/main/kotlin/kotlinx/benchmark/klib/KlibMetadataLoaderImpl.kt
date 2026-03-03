package kotlinx.benchmark.klib

import kotlinx.metadata.klib.KlibModuleMetadata
import org.jetbrains.kotlin.library.resolveSingleFileKlib
import java.io.File

public class KlibMetadataLoaderImpl : KlibMetadataLoader {
    public override fun load(klibFile: File): KlibModuleMetadata {
        val library = resolveSingleFileKlib(org.jetbrains.kotlin.konan.file.File(klibFile.absolutePath))

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

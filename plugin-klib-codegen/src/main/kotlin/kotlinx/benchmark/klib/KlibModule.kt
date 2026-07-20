package kotlinx.benchmark.klib

import kotlinx.metadata.klib.KlibModuleMetadata
import org.jetbrains.kotlin.library.components.metadata
import org.jetbrains.kotlin.library.loader.KlibLoader
import java.io.File

@KotlinxBenchmarkCodegenInternalApi
public class KlibModule internal constructor(internal val metadata: KlibModuleMetadata) {
    @KotlinxBenchmarkCodegenInternalApi
    public companion object {
        public fun loadWebModules(klib: File): List<KlibModule> {
            //skip processing of empty dirs (fail if not to do it)
            if (klib.listFiles() == null) return emptyList()
            return listOf(loadKlibModuleMetadata(klib)).map { KlibModule(it) }
        }

        public fun loadModules(lib: File): KlibModule = KlibModule(loadKlibModuleMetadata(lib))
    }
}

private fun loadKlibModuleMetadata(lib: File): KlibModuleMetadata {
    val resolvedLibrary = KlibLoader {
        libraryPaths(lib)
    }.load().librariesStdlibFirst.first()
    val metadata = KlibModuleMetadata.read(object : KlibModuleMetadata.MetadataLibraryProvider {
        override val moduleHeaderData: ByteArray
            get() = resolvedLibrary.metadata.moduleHeaderData

        override fun packageMetadata(fqName: String, partName: String): ByteArray =
            resolvedLibrary.metadata.getPackageFragment(fqName, partName)

        override fun packageMetadataParts(fqName: String): Set<String> =
            resolvedLibrary.metadata.getPackageFragmentNames(fqName)
    })
    return metadata
}

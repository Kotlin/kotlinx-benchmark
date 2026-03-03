package kotlinx.benchmark.klib

import kotlinx.metadata.klib.KlibModuleMetadata
import java.io.File

public interface KlibMetadataLoader {
    public fun load(klibFile: File): KlibModuleMetadata
}

public object KlibMetadataLoaderFactory {
    public fun create(): KlibMetadataLoader {
        val loaderClass = "kotlinx.benchmark.klib.KlibMetadataLoaderImpl"
        return Class.forName(loaderClass).getDeclaredConstructor().newInstance() as KlibMetadataLoader
    }
}

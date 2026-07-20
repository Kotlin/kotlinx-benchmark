package kotlinx.benchmark.klib

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import java.io.File

public class KlibModule(internal val moduleDescriptor: ModuleDescriptor) {
    public companion object {
        fun loadJsModules(klib: File, dependencies: Set<File>): List<KlibModule> {
            return loadJsIr(
                lib = klib,
                inputDependencies = dependencies,
                storageManager = LockBasedStorageManager("Inspect"),
            ).map { KlibModule(it) }
        }

        fun loadWasmModules(klib: File, dependencies: Set<File>): List<KlibModule> {
            return loadWasmIr(
                klib,
                inputDependencies = dependencies,
                LockBasedStorageManager("Inspect"),
            ).map { KlibModule(it) }
        }

        fun loadNativeModule(lib: File, dependencies: Set<File>): KlibModule {
            val storageManager = LockBasedStorageManager("Inspect")
            val module =
                KlibResolver.Native.createModuleDescriptor(lib, dependencies, storageManager)
            return KlibModule(module)
        }
    }
}

private fun loadWasmIr(
    lib: File,
    inputDependencies: Set<File>,
    storageManager: StorageManager,
): List<ModuleDescriptor> {
    //skip processing of empty dirs (fail if not to do it)
    if (lib.listFiles() == null) return emptyList()
    val dependencies = inputDependencies.filterNot { it.extension == "js" }.toSet()
    val module = KlibResolver.JS.createModuleDescriptor(lib, dependencies, storageManager)
    return listOf(module)
}

private fun loadJsIr(
    lib: File,
    inputDependencies: Set<File>,
    storageManager: StorageManager,
): List<ModuleDescriptor> {
    // skip processing of empty dirs (fails if not to do it)
    if (lib.listFiles() == null) return emptyList()
    val dependencies = inputDependencies.filterNot { it.extension == "js" }.toSet()
    val module = KlibResolver.JS.createModuleDescriptor(lib, dependencies, storageManager)
    return listOf(module)
}

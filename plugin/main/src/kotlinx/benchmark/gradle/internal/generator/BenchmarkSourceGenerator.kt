package kotlinx.benchmark.gradle.internal.generator

import com.google.devtools.ksp.impl.KotlinSymbolProcessing
import com.google.devtools.ksp.processing.*
import kotlinx.benchmark.gradle.Platform
import java.io.File

internal object BenchmarkSourceGenerator {
    internal fun generate(
        title: String,
        platform: Platform,
        sourceRoots: List<File>,
        inputDependencies: List<File>,
        outputSourcesDir: File,
        outputResourcesDir: File,
        outputClassesDir: File,
        projectDir: File,
        outputBaseDir: File,
        cachesDir: File,
        languageVersion: String,
        apiVersion: String,
        nativeTarget: String,
    ) {
        outputBaseDir.deleteRecursively()
        outputSourcesDir.mkdirs()
        outputResourcesDir.mkdirs()
        outputClassesDir.mkdirs()

        val kspConfig = when (platform) {
            Platform.NativeBuiltIn -> KSPNativeConfig.Builder().apply {
                setupCommon(
                    title,
                    sourceRoots,
                    inputDependencies,
                    projectDir,
                    outputBaseDir,
                    cachesDir,
                    outputClassesDir,
                    outputSourcesDir,
                    outputResourcesDir,
                    languageVersion,
                    apiVersion,
                )
                target = nativeTarget
            }.build()

            Platform.JsBuiltIn,
            Platform.JsBenchmarkJs,
            Platform.WasmBuiltIn -> KSPJsConfig.Builder().apply {
                setupCommon(
                    title,
                    sourceRoots,
                    inputDependencies,
                    projectDir,
                    outputBaseDir,
                    cachesDir,
                    outputClassesDir,
                    outputSourcesDir,
                    outputResourcesDir,
                    languageVersion,
                    apiVersion,
                )
                backend = if (platform == Platform.WasmBuiltIn) "WASM" else "JS"
            }.build()
        }

        val exitCode = KotlinSymbolProcessing(
            kspConfig,
            listOf(BenchmarkSymbolProcessorProvider(title, platform)),
            KspGradleLogger(KspGradleLogger.LOGGING_LEVEL_LOGGING)
        ).execute()
        check(exitCode == KotlinSymbolProcessing.ExitCode.OK) {
            "Error running KSP: $exitCode"
        }
    }

    private fun KSPConfig.Builder.setupCommon(
        title: String,
        sourceRootsValue: List<File>,
        inputDependencies: List<File>,
        projectDir: File,
        outputBaseDirValue: File,
        cachesDirValue: File,
        outputClassesDir: File,
        outputSourcesDir: File,
        outputResourcesDir: File,
        languageVersionValue: String,
        apiVersionValue: String,
    ) {
        moduleName = title
        sourceRoots = sourceRootsValue
        libraries = inputDependencies
        friends = emptyList()
        projectBaseDir = projectDir
        outputBaseDir = outputBaseDirValue
        cachesDir = cachesDirValue
        classOutputDir = outputClassesDir
        kotlinOutputDir = outputSourcesDir
        resourceOutputDir = outputResourcesDir
        languageVersion = languageVersionValue
        apiVersion = apiVersionValue
    }
}

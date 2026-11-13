package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.internal.BenchmarksPluginConstants
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import kotlin.text.replaceFirstChar

@KotlinxBenchmarkPluginExperimentalApi
public class CustomEngine(
    public val name: String,
    public val enginePath: Provider<RegularFile>,
    public val workingDir: Provider<Directory>? = null,
    public val engineArguments: Provider<List<String>>? = null,
)

public open class BenchmarkConfiguration
internal constructor(
    internal val extension: BenchmarksExtension,
    public val name: String,
) {
    public var iterations: Int? = null
    public var warmups: Int? = null
    public var iterationTime: Long? = null
    public var iterationTimeUnit: String? = null
    public var mode: String? = null
    public var outputTimeUnit: String? = null
    public var reportFormat: String? = null
    public var threads: Int? = null

    public var includes: MutableList<String> = mutableListOf()
    public var excludes: MutableList<String> = mutableListOf()
    public var params: MutableMap<String, MutableList<Any?>> = mutableMapOf()
    public var advanced: MutableMap<String, Any?> = mutableMapOf()

    @KotlinxBenchmarkPluginExperimentalApi
    public var customEngine: CustomEngine? = null

    public fun include(pattern: String) {
        includes.add(pattern)
    }

    public fun exclude(pattern: String) {
        excludes.add(pattern)
    }

    public fun param(name: String, vararg value: Any?) {
        val values = params.getOrPut(name) { mutableListOf() }
        values.addAll(value)
    }

    public fun advanced(name: String, value: Any?) {
        advanced[name] = value
    }

    internal fun capitalizedName() = if (name == "main") "" else name.replaceFirstChar { it.titlecase() }

    internal fun prefixName(suffix: String) = if (name == "main") suffix else name + suffix.replaceFirstChar { it.titlecase() }

    internal fun reportFileExt(): String = reportFormat?.lowercase() ?: "json"
}

public open class BenchmarkTarget
internal constructor(
    internal val extension: BenchmarksExtension,
    public val name: String,
) {
    public var workingDir: String? = null
}

public abstract class JvmBenchmarkTarget
internal constructor(
    extension: BenchmarksExtension,
    name: String
) : BenchmarkTarget(extension, name) {
    public var jmhVersion: String = extension.project.providers.gradleProperty("benchmarks_jmh_version")
        .orElse(BenchmarksPluginConstants.DEFAULT_JMH_VERSION).get()
}

public class JavaBenchmarkTarget
internal constructor(
    extension: BenchmarksExtension,
    name: String,
    internal val sourceSet: SourceSet
) : JvmBenchmarkTarget(extension, name)

public open class KotlinJvmBenchmarkTarget
internal constructor(
    extension: BenchmarksExtension,
    name: String,
    internal val compilation: KotlinJvmCompilation
) : JvmBenchmarkTarget(extension, name)

public enum class JsBenchmarksExecutor {
    BenchmarkJs,
    BuiltIn
}

public class JsBenchmarkTarget
internal constructor(
    extension: BenchmarksExtension,
    name: String,
    internal val compilation: KotlinJsIrCompilation
) : BenchmarkTarget(extension, name) {
    public var jsBenchmarksExecutor: JsBenchmarksExecutor = JsBenchmarksExecutor.BenchmarkJs
    public var buildType: KotlinJsBinaryMode = KotlinJsBinaryMode.PRODUCTION
}

public class WasmBenchmarkTarget
internal constructor(
    extension: BenchmarksExtension,
    name: String,
    internal val compilation: KotlinJsIrCompilation
) : BenchmarkTarget(extension, name) {
    public var buildType: KotlinJsBinaryMode = KotlinJsBinaryMode.PRODUCTION
}

public class NativeBenchmarkTarget
internal constructor(
    extension: BenchmarksExtension,
    name: String,
    internal val compilation: KotlinNativeCompilation
) : BenchmarkTarget(extension, name) {
    public var buildType: NativeBuildType = NativeBuildType.RELEASE
}

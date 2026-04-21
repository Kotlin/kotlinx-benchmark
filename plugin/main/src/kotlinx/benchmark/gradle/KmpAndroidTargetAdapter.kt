package kotlinx.benchmark.gradle

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

/**
 * Android Support in Kotlin Multiplatform has changed significantly over time. This means
 * that extracting the information `kotlinx-benchmark` need from the KMP target can be tricky.
 *
 * [KmpAndroidTargetCompat] was introduced as a facade to hide this. Its purpose is to provide a
 * consistent interface for accessing Android target properties across different versions
 * of the Android Gradle Plugin.
 *
 * Right now this compatibility layer supports the following scenarios:
 *
 * 1. Plugin `id("com.android.kotlin.multiplatform.library")`
 *    a. AGP < 9.0: `com.android.build.api.dsl.KotlinMultiplatformAndroidTarget`
 *    b. AGP 9.0+: `com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget`
 *
 * We could also extend this support to `com.android.library` and `com.android.application`
 * targets, but since these are deprecated for KMP, they are left out for now.
 */
internal class KmpAndroidTargetCompat(
    val namespace: String?,
    val compileSdk: Int?,
    val minSdk: Int?,
)

private const val AGP_TARGET_CLASS_PRE9 = "com.android.build.api.dsl.KotlinMultiplatformAndroidTarget"
private const val AGP_TARGET_CLASS_9PLUS = "com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget"
private const val AGP_COMPILATION_CLASS = "com.android.build.api.dsl.KotlinMultiplatformAndroidCompilation"
private const val AGP_VERSION_CLASS = "com.android.build.api.AndroidPluginVersion"
private const val AGP_COMPONENTS_CLASS = "com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension"
private val AGP_TARGET_CLASSES = listOf(
    AGP_TARGET_CLASS_PRE9,
    AGP_TARGET_CLASS_9PLUS
)

/**
 * Convert any supported Android [KotlinTarget] to [KmpAndroidTargetCompat].
 * If this is not possible, an exception is thrown.
 */
internal fun Any.toKmpAndroidTargetCompat(): KmpAndroidTargetCompat {
    val agpTargetSupported = AGP_TARGET_CLASSES.any { agpTargetFqn ->
        val agpTargetClass = tryGetClass<Any>(agpTargetFqn)
        agpTargetClass?.isInstance(this) == true
    }
    if (!agpTargetSupported) {
        throw GradleException(
            "Cannot read Android target properties: expected an instance of " +
                    "$AGP_TARGET_CLASS_PRE9 or $AGP_TARGET_CLASS_9PLUS, " +
                    "but got ${this::class.qualifiedName}"
        )
    }

    val namespace = javaClass.getMethod("getNamespace").invoke(this) as? String
    val compileSdk = javaClass.getMethod("getCompileSdk").invoke(this) as? Int
    val minSdk = javaClass.getMethod("getMinSdk").invoke(this) as? Int

    return KmpAndroidTargetCompat(namespace, compileSdk, minSdk)
}

internal fun KotlinCompilation<*>.isKmpAndroidCompilation(): Boolean {
    val compilationClass = tryGetClass<Any>(AGP_COMPILATION_CLASS) ?: return false
    return compilationClass.isInstance(this)
}

/**
 * Returns the current AGP version string from `AndroidPluginVersion`, e.g. "8.6.1".
 *
 * - `getCurrent()` was added in 8.3, given that we only support back to 8.2, we can just set it to that if not found.
 * - `getVersion()` was added in 8.4, if not available, fall back to constructing it manually.
 *
 * See https://developer.android.com/reference/tools/gradle-api/8.2/com/android/build/api/AndroidPluginVersion
 * See https://developer.android.com/reference/tools/gradle-api/8.3/com/android/build/api/AndroidPluginVersion
 * See https://developer.android.com/reference/tools/gradle-api/8.4/com/android/build/api/AndroidPluginVersion
 */
internal fun getAgpVersion(classLoader: ClassLoader): String {
    val versionClass = Class.forName(AGP_VERSION_CLASS, false, classLoader)
    val getCurrentMethod = try {
        versionClass.getMethod("getCurrent")
    } catch (_: NoSuchMethodException) {
        // 8.2.1 and 8.2.2 also exist, but selecting the oldest version should be safe.
        return "8.2.0"
    }
    val current = getCurrentMethod.invoke(null)
    return runCatching { current.javaClass.getMethod("getVersion").invoke(current) as String }
        .getOrElse {
            val major = current.javaClass.getMethod("getMajor").invoke(current) as Int
            val minor = current.javaClass.getMethod("getMinor").invoke(current) as Int
            val micro = current.javaClass.getMethod("getMicro").invoke(current) as Int
            "$major.$minor.$micro"
        }
}

/**
 * Returns the `Provider<RegularFile>` pointing to the ADB executable.
 * This API was added in AGP 4.2, so should be safe across all supported versions.
 *
 * @param agpClassLoader a classloader that has AGP on it (e.g. `compilation.javaClass.classLoader`)
 */
internal fun Project.getAndroidAdb(agpClassLoader: ClassLoader): Provider<RegularFile> {
    val extensionClass = Class.forName(AGP_COMPONENTS_CLASS, false, agpClassLoader) as Class<Any>
    val extension = extensions.getByType(extensionClass)
    val sdkComponents = extension.javaClass.getMethod("getSdkComponents").invoke(extension)
    @Suppress("UNCHECKED_CAST")
    return sdkComponents.javaClass.getMethod("getAdb").invoke(sdkComponents) as Provider<RegularFile>
}

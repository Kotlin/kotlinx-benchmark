package kotlinx.benchmark.gradle.internal

import org.gradle.api.artifacts.Configuration
import org.gradle.util.GradleVersion

/** `true` if [Configuration.isCanBeDeclared] is supported by the current Gradle version. */
@Suppress("UnstableApiUsage")
private val isCanBeDeclaredSupported = GradleVersion.current() >= GradleVersion.version("8.2")

// Remove when minimum supported Gradle version is >= 8.2
@Suppress("UnstableApiUsage")
internal var Configuration.isCanBeDeclaredCompat: Boolean
    get() = if (isCanBeDeclaredSupported) isCanBeDeclared else false
    set(value) {
        if (isCanBeDeclaredSupported) isCanBeDeclared = value
    }

/** `true` if [Configuration.isVisible] is still available in the current Gradle version. */
private val isVisibleSupported = GradleVersion.current() < GradleVersion.version("9.0")

internal var Configuration.isVisibleCompat: Boolean
    get() = if (isVisibleSupported) isVisible else false
    set(value) {
        if (isVisibleSupported) isVisible = value
    }

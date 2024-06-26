package kotlinx.benchmarks_build.utils

import org.gradle.api.artifacts.*


/**
 * Mark this [Configuration] as one that should be used to declare dependencies in
 * [org.gradle.api.Project.dependencies] block.
 *
 * Declarable Configurations should be extended by [resolvable] and [consumable] Configurations.
 * They must not have attributes.
 *
 * ```
 * isCanBeResolved = false
 * isCanBeConsumed = false
 * isCanBeDeclared = true
 * ```
 */
internal fun Configuration.declarable(
    visible: Boolean = false,
) {
    isCanBeResolved = false
    isCanBeConsumed = false
    @Suppress("UnstableApiUsage")
    isCanBeDeclared = true
    isVisible = visible
}


/**
 * Mark this [Configuration] as one that will be consumed by other subprojects.
 *
 * Consumable Configurations must extend a [declarable] Configuration.
 * They should have attributes.
 *
 * ```
 * isCanBeResolved = false
 * isCanBeConsumed = true
 * isCanBeDeclared = false
 * ```
 */
internal fun Configuration.consumable(
    visible: Boolean = false,
) {
    isCanBeResolved = false
    isCanBeConsumed = true
    @Suppress("UnstableApiUsage")
    isCanBeDeclared = false
    isVisible = visible
}


/**
 * Mark this [Configuration] as one that will consume artifacts from other subprojects (also known as 'resolving')
 *
 * Resolvable Configurations should have attributes.
 *
 * ```
 * isCanBeResolved = true
 * isCanBeConsumed = false
 * isCanBeDeclared = false
 * ```
 */
internal fun Configuration.resolvable(
    visible: Boolean = false,
) {
    isCanBeResolved = true
    isCanBeConsumed = false
    @Suppress("UnstableApiUsage")
    isCanBeDeclared = false
    isVisible = visible
}

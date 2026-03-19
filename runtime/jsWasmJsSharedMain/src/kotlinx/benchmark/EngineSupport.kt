package kotlinx.benchmark

@KotlinxBenchmarkRuntimeExperimentalApi
/**
 * Defines a platform-specific measurement lifecycle for benchmark execution.
 *
 * Implementations are expected to:
 * - mark the beginning of a measured section in [measureStart];
 * - return elapsed time for that section from [measureFinish].
 *
 * The returned value is interpreted as nanoseconds.
 */
abstract class Measurer {
    /** Marks the start of a measured benchmark section. */
    abstract fun measureStart()

    /**
     * Marks the end of measurement and returns elapsed time.
     *
     * @return elapsed time in nanoseconds.
     */
    abstract fun measureFinish(): Long
}

@KotlinxBenchmarkRuntimeExperimentalApi
/**
 * Provides platform-specific runtime services required by benchmark execution.
 *
 * Implementations are responsible for:
 * - reading and writing files used by the runner;
 * - supplying runtime arguments;
 * - providing a [Measurer] implementation;
 * - reporting whether the current engine is supported.
 */
abstract class BenchmarkEngineSupport {
    /** Reads text content from [path]. */
    abstract fun readFile(path: String): String

    /** Writes [content] as text to [path]. */
    abstract fun writeFile(path: String, content: String)

    /** Returns runtime arguments passed to the benchmark process. */
    abstract fun arguments(): Array<out String>

    /** Creates a measurer used for benchmark timing. */
    abstract fun getMeasurer(): Measurer

    /** Returns `true` if this engine support can run in the current environment. */
    abstract fun isSupported(): Boolean
}

@KotlinxBenchmarkRuntimeExperimentalApi
/**
 * Replaces the current benchmark engine support with [overriddenSupport].
 *
 * The provided support must be valid for the current environment.
 *
 * @throws IllegalStateException if [overriddenSupport] is not supported.
 */
fun overrideEngineSupport(overriddenSupport: BenchmarkEngineSupport) {
    check(overriddenSupport.isSupported()) { "Engine is not supported" }
    engineSupport = overriddenSupport
}

internal expect var engineSupport: BenchmarkEngineSupport

internal actual fun String.readFile(): String =
    engineSupport.readFile(this)

internal actual fun String.writeFile(text: String): Unit =
    engineSupport.writeFile(this, text)
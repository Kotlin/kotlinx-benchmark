package kotlinx.benchmark.integration

/**
 * Utility class, bridging the difference in Java Runtime API's that have changed
 * over time.
 */
object JavaRuntimeUtil {
    /**
     * Returns the "feature" version for Java 9+ or the "major" version for
     * Java runtimes below 9.
     *
     * Will throw an exception if the Feature version could not be determined.
     */
    fun getFeatureVersion(): Int {
        return try {
            // `Runtime.version()` is available on Java 9+.
            // `Version.feature()` was added in Java 10 where `Version.major()`
            // was deprecated. We use `.major()` to cover as many versions as
            // possible.
            val versionObj = Runtime::class.java.getMethod("version").invoke(null)
            val feature = versionObj.javaClass.getMethod("major").invoke(versionObj) as Int
            feature
        } catch (_: Exception) {
            // Fallback to parse Java 8 version strings and below
            val version = System.getProperty("java.version")
            val parts = version.split('.')
            if (parts.size != 3 || parts[0] != "1") {
                throw IllegalStateException("Unrecognized Java version string: $version");
            }
            parts[1].toInt()
        }
    }
}

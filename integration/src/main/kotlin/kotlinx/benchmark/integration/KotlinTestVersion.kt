package kotlinx.benchmark.integration

enum class KotlinTestVersion(val versionString: String) {
    MinSupportedKotlinVersion("2.0.0"),
    UnsupportedKotlinVersion("1.9.20"),
    Kotlin2_3_0("2.3.0");

    companion object {
        fun overriddenVersion(): String? {
            val prop = System.getProperty("kotlin_version")
            if (prop.isNullOrBlank()) return null
            return prop
        }

        fun mostRecent(versionStringA: String, versionStringB: String): String {
            val versionA = parseVersion(versionStringA)
            val versionB = parseVersion(versionStringB)
            if (versionB > versionA) return versionStringB
            return versionStringA
        }

        // copied from the plugin
        private fun parseVersion(version: String): KotlinVersion {
            val (major, minor) = version
                .split('.')
                .take(2)
                .map { it.toInt() }
            val patch = version.substringAfterLast('.').substringBefore('-').toInt()
            return KotlinVersion(major, minor, patch)
        }
    }
}

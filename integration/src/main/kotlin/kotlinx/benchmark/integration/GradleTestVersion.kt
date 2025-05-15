package kotlinx.benchmark.integration

enum class GradleTestVersion(val versionString: String) {
    v8_0("8.0.2"),
    MinSupportedGradleVersion(System.getProperty("minSupportedGradleVersion")),
    UnsupportedGradleVersion("7.3"),
    MinSupportedKotlinVersion("2.1.21"),
    UnsupportedKotlinVersion("1.9.20"),
}

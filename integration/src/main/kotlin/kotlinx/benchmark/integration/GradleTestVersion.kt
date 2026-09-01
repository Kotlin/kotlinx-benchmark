package kotlinx.benchmark.integration

enum class GradleTestVersion(val versionString: String) {
    v8_14_5("8.14.5"),
    MinSupportedGradleVersion(System.getProperty("minSupportedGradleVersion")),
    UnsupportedGradleVersion("7.3"),
    v9_3_0("9.3.0"),
}

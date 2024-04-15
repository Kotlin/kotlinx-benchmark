package kotlinx.benchmark.integration

enum class GradleTestVersion(val versionString: String) {
    v7("7.6.4"),
    v8_0("8.0.2"),
    MinSupportedGradleVersion("7.4"),
    UnsupportedGradleVersion("7.3"),
}

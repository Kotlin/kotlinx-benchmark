package kotlinx.benchmark.integration

enum class GradleTestVersion(val versionString: String) {
    v8_0("8.0.2"),
    v8_2("8.2"),
    v8_7("8.7"),
    v8_10_2("8.10.2"),
    MinSupportedGradleVersion(System.getProperty("minSupportedGradleVersion")),
    UnsupportedGradleVersion("7.3"),
    v9_3_0("9.3.0"),
    v9_3_1("9.3.1"),
    v9_4_1("9.4.1")
}

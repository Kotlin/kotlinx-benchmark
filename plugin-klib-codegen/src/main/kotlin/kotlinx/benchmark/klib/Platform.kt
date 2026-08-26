package kotlinx.benchmark.klib

public enum class Platform(
    internal val runBenchmarks: String,
    internal val suiteDescriptorClass: String,
    internal val benchmarkDescriptorClass: String,
    internal val benchmarkDescriptorWithBlackholeParameterClass: String
) {
    JsBuiltIn(
        runBenchmarks = "kotlinx.benchmark.js.runBenchmarksBuiltIn",
        suiteDescriptorClass = "kotlinx.benchmark.SuiteDescriptor",
        benchmarkDescriptorClass = "kotlinx.benchmark.js.JsBenchmarkDescriptorWithNoBlackholeParameter",
        benchmarkDescriptorWithBlackholeParameterClass = "kotlinx.benchmark.js.JsBenchmarkDescriptorWithBlackholeParameter",
    ),
    JsBenchmarkJs(
        runBenchmarks = "kotlinx.benchmark.js.runBenchmarks",
        suiteDescriptorClass = "kotlinx.benchmark.SuiteDescriptor",
        benchmarkDescriptorClass = "kotlinx.benchmark.js.JsBenchmarkDescriptorWithNoBlackholeParameter",
        benchmarkDescriptorWithBlackholeParameterClass = "kotlinx.benchmark.js.JsBenchmarkDescriptorWithBlackholeParameter",
    ),
    NativeBuiltIn(
        runBenchmarks = "kotlinx.benchmark.native.runBenchmarks",
        suiteDescriptorClass = "kotlinx.benchmark.SuiteDescriptor",
        benchmarkDescriptorClass = "kotlinx.benchmark.BenchmarkDescriptorWithNoBlackholeParameter",
        benchmarkDescriptorWithBlackholeParameterClass = "kotlinx.benchmark.BenchmarkDescriptorWithBlackholeParameter",
    ),
    WasmBuiltIn(
        runBenchmarks = "kotlinx.benchmark.wasm.runBenchmarks",
        suiteDescriptorClass = "kotlinx.benchmark.SuiteDescriptor",
        benchmarkDescriptorClass = "kotlinx.benchmark.BenchmarkDescriptorWithNoBlackholeParameter",
        benchmarkDescriptorWithBlackholeParameterClass = "kotlinx.benchmark.BenchmarkDescriptorWithBlackholeParameter",
    )
}

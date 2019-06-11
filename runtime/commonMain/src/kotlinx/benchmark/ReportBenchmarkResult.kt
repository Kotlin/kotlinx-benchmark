package kotlinx.benchmark

class ReportBenchmarkResult(
    val config: BenchmarkConfiguration,
    val benchmark: BenchmarkDescriptor<*>,
    val params: Map<String, String>,
    val score: Double,
    val error: Double,
    val confidence: Pair<Double, Double>,
    val percentiles: Map<Double, Double>,
    val values: DoubleArray
)

package kotlinx.benchmark

class BenchmarkConfiguration(val runner: RunnerConfiguration, val suite: SuiteDescriptor<*>) {
    val iterations: Int get() = runner.iterations ?: suite.iterations
    val warmups: Int get() = runner.warmups ?: suite.warmups
    val iterationTime: Long get() = runner.iterationTime ?: suite.iterationTime.value
    val iterationTimeUnit: BenchmarkTimeUnit get() = runner.iterationTimeUnit ?: suite.iterationTime.timeUnit
    val outputTimeUnit: BenchmarkTimeUnit get() = runner.outputTimeUnit ?: suite.outputTimeUnit
    val mode: Mode get() = runner.mode ?: suite.mode
    val iterationMode: IterationMode = runner.iterationMode ?: IterationMode.External
}
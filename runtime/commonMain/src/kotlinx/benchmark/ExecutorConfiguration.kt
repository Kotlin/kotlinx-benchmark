package kotlinx.benchmark

class BenchmarkConfiguration private constructor(
    val iterations: Int,
    val warmups: Int,
    val iterationTime: Long,
    val iterationTimeUnit: BenchmarkTimeUnit,
    val outputTimeUnit: BenchmarkTimeUnit,
    val mode: Mode,
    val nativeFork: NativeFork,
    val nativeGCCollectMode: NativeGCCollectMode) {

    constructor(runner: RunnerConfiguration, suite: SuiteDescriptor<*>) : this(
        runner.iterations ?: suite.iterations,
        runner.warmups ?: suite.warmups,
        runner.iterationTime ?: suite.iterationTime.value,
        runner.iterationTimeUnit ?: suite.iterationTime.timeUnit,
        runner.outputTimeUnit ?: suite.outputTimeUnit,
        runner.mode ?: suite.mode,
        runner.nativeFork ?: NativeFork.PerBenchmark,
        runner.nativeGCCollectMode ?: NativeGCCollectMode.Auto
    )

    override fun toString() =
        "iterations=$iterations, warmups=$warmups, iterationTime=$iterationTime, " +
                "iterationTimeUnit=${iterationTimeUnit.toText()}, outputTimeUnit=${outputTimeUnit.toText()}, " +
                "mode=${mode.toText()}, nativeFork=${nativeFork.toText()}, " +
                "nativeGCCollectMode=${nativeGCCollectMode.toText()}"

    companion object {
        fun parse(description: String): BenchmarkConfiguration {
            val parameters = description.parseMap()

            fun getParameterValue(key: String) =
                parameters[key] ?: throw NoSuchElementException("Parameter `$key` is required.")

            return BenchmarkConfiguration(
                getParameterValue("iterations").toInt(),
                getParameterValue("warmups").toInt(),
                getParameterValue("iterationTime").toLong(),
                parseTimeUnit(getParameterValue("iterationTimeUnit")),
                parseTimeUnit(getParameterValue("outputTimeUnit")),
                getParameterValue("mode").toMode(),
                NativeFork.valueOf(getParameterValue("nativeFork").replaceFirstChar { it.uppercaseChar() }),
                NativeGCCollectMode.valueOf(getParameterValue("nativeGCCollectMode").replaceFirstChar { it.uppercaseChar() })
            )
        }
    }
}

internal fun String.parseMap(): Map<String, String> =
    this.removeSurrounding("{", "}").split(", ").filter{ it.isNotEmpty() }.map {
        val keyValue = it.split("=")
        require(keyValue.size == 2) { "Wrong format of map string description!" }
        val (key, value) = keyValue
        key to value
    }.toMap()
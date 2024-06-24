package kotlinx.benchmark.js

import kotlinx.benchmark.*
import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi
import kotlin.js.Promise

@KotlinxBenchmarkRuntimeInternalApi
class JsBenchmarkExecutor(name: String, @Suppress("UNUSED_PARAMETER") dummy_args: Array<out String>) :
    SuiteExecutor(name, jsEngineSupport.arguments()[0]) {

    init {
        check(!isD8) { "${JsBenchmarkExecutor::class.simpleName} does not support d8 engine" }
    }

    private val benchmarkJs: dynamic = require("benchmark")

    override fun run(
        runnerConfiguration: RunnerConfiguration,
        benchmarks: List<BenchmarkDescriptor<Any?>>,
        start: () -> Unit,
        complete: () -> Unit
    ) {
        start()
        val jsSuite: dynamic = benchmarkJs.Suite()
        jsSuite.on("complete") {
            complete()
        }

        benchmarks.forEach { benchmark ->
            val suite = benchmark.suite
            val config = BenchmarkConfiguration(runnerConfiguration, suite)
            val isAsync = benchmark.isAsync

            runWithParameters(suite.parameters, runnerConfiguration.params, suite.defaultParameters) { params ->
                val id = id(benchmark.name, params)

                val instance = suite.factory() // TODO: should we create instance per bench or per suite?
                suite.parametrize(instance, params)
                val asynchronous = if (isAsync) {
                    when(benchmark) {
                        // Mind asDynamic: this is **not** a regular promise
                        is JsBenchmarkDescriptorWithNoBlackholeParameter -> {
                            @Suppress("UNCHECKED_CAST")
                            val promiseFunction = benchmark.function as Any?.() -> Promise<*>
                            jsSuite.add(benchmark.name) { deferred: Promise<Unit> ->
                                instance.promiseFunction().then { (deferred.asDynamic()).resolve() }
                            }
                        }
                        is JsBenchmarkDescriptorWithBlackholeParameter -> {
                            @Suppress("UNCHECKED_CAST")
                            val promiseFunction = benchmark.function as Any?.(Blackhole) -> Promise<*>
                            jsSuite.add(benchmark.name) { deferred: Promise<Unit> ->
                                instance.promiseFunction(benchmark.blackhole).then { (deferred.asDynamic()).resolve() }
                            }
                        }
                        else -> error("Unexpected ${benchmark::class.simpleName}")
                    }
                    true
                } else {
                    when(benchmark) {
                        is JsBenchmarkDescriptorWithNoBlackholeParameter -> {
                            val function = benchmark.function
                            jsSuite.add(benchmark.name) { instance.function() }
                        }
                        is JsBenchmarkDescriptorWithBlackholeParameter -> {
                            val function = benchmark.function
                            jsSuite.add(benchmark.name) { instance.function(benchmark.blackhole) }
                        }
                        else -> error("Unexpected ${benchmark::class.simpleName}")
                    }
                    false
                }

                val jsBenchmark = jsSuite[jsSuite.length - 1] // take back last added benchmark and subscribe to events

                // TODO: Configure properly
                // initCount: The default number of times to execute a test on a benchmark’s first cycle
                // minTime: The time needed to reduce the percent uncertainty of measurement to 1% (secs).
                // maxTime: The maximum time a benchmark is allowed to run before finishing (secs).

                jsBenchmark.options.initCount = config.warmups
                jsBenchmark.options.minSamples = config.iterations
                val iterationSeconds = config.iterationTime * config.iterationTimeUnit.toSecondsMultiplier()
                jsBenchmark.options.minTime = iterationSeconds
                jsBenchmark.options.maxTime = iterationSeconds
                jsBenchmark.options.async = asynchronous
                jsBenchmark.options.defer = asynchronous

                jsBenchmark.on("start") { _ ->
                    reporter.startBenchmark(executionName, id)
                    suite.setup(instance)
                }
                var iteration = 0
                jsBenchmark.on("cycle") { event ->
                    val target = event.target
                    val nanos = (target.times.period as Double) * BenchmarkTimeUnit.SECONDS.toMultiplier()
                    val sample = nanos.nanosToText(config.mode, config.outputTimeUnit)
                    // (${target.cycles} × ${target.count} calls) -- TODO: what's this?
                    reporter.output(
                        executionName,
                        id,
                        "Iteration #${iteration++}: $sample"
                    )
                }
                jsBenchmark.on("complete") { event ->
                    suite.teardown(instance)
                    benchmark.blackhole.flush()
                    val stats = event.target.stats
                    val samples = stats.sample
                        .unsafeCast<DoubleArray>()
                        .map {
                            val nanos = it * BenchmarkTimeUnit.SECONDS.toMultiplier()
                            nanos.nanosToSample(config.mode, config.outputTimeUnit)
                        }
                        .toDoubleArray()
                    val result = ReportBenchmarksStatistics.createResult(benchmark, params, config, samples)
                    val message = with(result) {
                        "  ~ ${score.sampleToText(
                            config.mode,
                            config.outputTimeUnit
                        )} ±${(error / score * 100).formatSignificant(2)}%"
                    }
                    val error = event.target.error
                    if (error == null) {
                        reporter.endBenchmark(
                            executionName,
                            id,
                            BenchmarkProgress.FinishStatus.Success,
                            message
                        )
                        result(result)
                    } else {
                        val stacktrace = error.stack
                        reporter.endBenchmarkException(
                            executionName,
                            id,
                            error.toString(),
                            stacktrace.toString()
                        )
                    }
                }
                Unit
            }
        }
        jsSuite.run()
    }
}

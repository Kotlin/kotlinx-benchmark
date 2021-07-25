package kotlinx.benchmark.js

import kotlinx.benchmark.*
import kotlin.js.Promise

class JsExecutor(name: String, @Suppress("UNUSED_PARAMETER") dummy_args: Array<out String>) :
    SuiteExecutor(name, (process["argv"] as Array<String>).drop(2).toTypedArray()) {
    
    private val benchmarkJs: dynamic = require("benchmark")

    override fun run(
        runnerConfiguration: RunnerConfiguration,
        reporter: BenchmarkProgress,
        benchmarks: List<BenchmarkDescriptor<Any?>>,
        complete: () -> Unit
    ) {
        val jsSuite: dynamic = benchmarkJs.Suite()
        jsSuite.on("complete") {
            complete()
        }

        benchmarks.forEach { benchmark ->
            val suite = benchmark.suite

            if (suite.hasInvocationFixture) {
                throw UnsupportedOperationException("Fixture methods with `Invocation` level are not supported")
            }

            val config = BenchmarkConfiguration(runnerConfiguration, suite)
            val jsDescriptor = benchmark as JsBenchmarkDescriptor

            runWithParameters(suite.parameters, runnerConfiguration.params, suite.defaultParameters) { params ->
                val id = id(benchmark.name, params)

                @Suppress("UNCHECKED_CAST")
                val function = benchmark.function
                val instance = suite.factory() // TODO: should we create instance per bench or per suite?
                suite.parametrize(instance, params)

                val asynchronous = if (jsDescriptor.async) {
                    @Suppress("UNCHECKED_CAST")
                    val promiseFunction = function as Any?.() -> Promise<*>

                    jsSuite.add(benchmark.name) { deferred: Promise<Unit> ->
                        // Mind asDynamic: this is **not** a regular promise
                        instance.promiseFunction().then { (deferred.asDynamic()).resolve() }
                    }
                    true
                } else {
                    jsSuite.add(benchmark.name) { instance.function() }
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

                jsBenchmark.on("start") { event ->
                    reporter.startBenchmark(executionName, id)
                    suite.trialSetup(instance)
                    suite.iterationSetup(instance)
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
                    suite.iterationTearDown(instance)
                    suite.iterationSetup(instance)
                }
                jsBenchmark.on("complete") { event ->
                    suite.iterationTearDown(instance)
                    suite.trialTearDown(instance)
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

external fun require(module: String): dynamic
private val process = require("process")


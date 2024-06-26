package test

import kotlinx.benchmark.*
import kotlin.js.*

@State(Scope.Benchmark)
class JsAsyncBenchmarks {

    private var data = 0.0

    @Setup
    fun setUp() {
        data = 42.0
    }

    @Benchmark
    fun baseline(): Double {
        // This benchmarks shows that benchmarks.js fails a baseline test :)
        return data
    }

    @Benchmark
    fun promiseResolve(): Promise<Double> {
        return Promise.resolve(data)
    }

    @Benchmark
    fun promiseResolveDoubleChain(): Promise<Double> {
        return Promise.resolve(Unit).then { data }
    }

    @Benchmark
    fun promiseResolveTripleChain(): Promise<Double> {
        return Promise.resolve(Unit).then { Unit }.then { data }
    }

    @Benchmark
    fun promiseDelayedBaseline(): Promise<Double> {
        // Score of this benchmark cannot be greater than 10 ops/sec
        return Promise { resolve, _ ->
            setTimeout({
                resolve(data)
            }, 100)
        }
    }

}

private external fun setTimeout(handler: dynamic, timeout: Int = definedExternally): Int

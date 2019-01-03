package org.jetbrains.gradle.benchmarks.native

class Suite {
    private val benchmarks  = mutableListOf<BenchmarkDescriptor>()
    
    fun add(name: String, function: () -> Unit) {
        benchmarks.add(BenchmarkDescriptor(name, function))
    }
    
    fun run() {
        println("Pretending to run benchmarks…")
        benchmarks.forEach { 
            println("Benchmark: ${it.name}")
            it.function()
        }
    }
}

class BenchmarkDescriptor(val name: String, val function: () -> Unit)

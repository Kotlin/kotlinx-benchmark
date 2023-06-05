| Task | Description |
|---|---|
| **assembleBenchmarks** | The task responsible for generating and building all benchmarks in the project. Serves as a dependency for other benchmark tasks. |
| **benchmark** | The primary task for executing all benchmarks in the project. Depends on `assembleBenchmarks` to ensure benchmarks are ready and built. |
| **{configName}Benchmark** | Executes all benchmarks under the specific configuration. Useful when different benchmarking requirements exist for different parts of the application. |
| **{configName}BenchmarkGenerate** | Generates JMH source files for the specified configuration. JMH is a benchmarking toolkit for Java and JVM-targeting languages. |
| **{configName}BenchmarkCompile** | Compiles the JMH source files generated for a specific configuration, transforming them into machine code for JVM execution. |
| **{configName}BenchmarkJar** | Packages the compiled JMH files into a JAR (Java Archive) file for distribution and execution. |
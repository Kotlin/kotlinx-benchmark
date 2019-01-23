package test;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class SampleJavaBenchmark {
    @Benchmark
    public String stringBuilder() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(10);
        return stringBuilder.toString();
    }
}

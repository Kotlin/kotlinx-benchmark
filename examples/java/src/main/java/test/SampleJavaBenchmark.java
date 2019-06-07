package test;

import org.openjdk.jmh.annotations.*;

@State(Scope.Benchmark)
@Fork(1)
public class SampleJavaBenchmark {
    @Param({"A", "B"})
    String stringValue;

    @Param({"1", "2"})
    int intValue;
    
    @Benchmark
    public String stringBuilder() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(10);
        stringBuilder.append(stringValue);
        stringBuilder.append(intValue);
        return stringBuilder.toString();
    }
}

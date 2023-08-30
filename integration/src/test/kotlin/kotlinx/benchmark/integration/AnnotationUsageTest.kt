package kotlinx.benchmark.integration

import kotlin.test.*

class AnnotationUsageTest : GradleTest() {
    @Test
    fun testParamWithVal() {
        val runner = project("kotlin-multiplatform")
        
        runner.addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
            param("valProperty", "1", "5")
        }

        runner.runAndFail("nativeBenchmark") {
            assertOutputContains("Invalid usage of @Param: Property `valProperty` is read-only (val). Ensure properties annotated with @Param are mutable (var).")
        }
        runner.runAndFail("wasmJsBenchmark") {
            assertOutputContains("Invalid usage of @Param: Property `valProperty` is read-only (val). Ensure properties annotated with @Param are mutable (var).")
        }
        runner.runAndFail("jsIrBenchmark") {
            assertOutputContains("Invalid usage of @Param: Property `valProperty` is read-only (val). Ensure properties annotated with @Param are mutable (var).")
        }
        runner.runAndFail("jvmBenchmark") {
            assertOutputContains("@Param annotation is not acceptable on final fields.")
        }
    }

    @Test
    fun testBenchmarkWithFinal() {
        val runner = project("kotlin-multiplatform")
        
        runner.addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
            benchmark("finalMethod")
        }

        runner.run("nativeBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
        runner.run("wasmJsBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
        runner.run("jsIrBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
        runner.run("jvmBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
    }

    @Test
    fun testBenchmarkWithPrivate() {
        val runner = project("kotlin-multiplatform")
        
        runner.addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
            benchmark("privateMethod")
        }

        runner.runAndFail("nativeBenchmark") {
            assertOutputContains("Invalid usage of @Benchmark: Method `privateMethod` is private. @Benchmark methods should be public.")
        }
        runner.runAndFail("wasmJsBenchmark") {
            assertOutputContains("Invalid usage of @Benchmark: Method `privateMethod` is private. @Benchmark methods should be public.")
        }
        runner.runAndFail("jsIrBenchmark") {
            assertOutputContains("Invalid usage of @Benchmark: Method `privateMethod` is private. @Benchmark methods should be public.")
        }
        runner.runAndFail("jvmBenchmark") {
            assertOutputContains("@Benchmark method should be public.")
        }
    }

    @Test
    fun testBenchmarkWithInternal() {
        val runner = project("kotlin-multiplatform")
        
        runner.addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
            benchmark("internalMethod")
        }

        runner.runAndFail("nativeBenchmark") {
            assertOutputContains("Invalid usage of @Benchmark: Method `internalMethod` is internal.")
        }
        runner.runAndFail("wasmJsBenchmark") {
            assertOutputContains("Invalid usage of @Benchmark: Method `internalMethod` is internal.")
        }
        runner.runAndFail("jsIrBenchmark") {
            assertOutputContains("Invalid usage of @Benchmark: Method `internalMethod` is internal.")
        }
        runner.run("jvmBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
    }

    @Test
    fun testSetupWithPrivate() {
        val runner = project("kotlin-multiplatform")
        
        runner.addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
            setup("privateMethod")
        }

        runner.runAndFail("nativeBenchmark") {
            assertOutputContains("Invalid usage of @Setup: Method `privateMethod` is private. @Setup methods should be public.")
        }
        runner.runAndFail("wasmJsBenchmark") {
            assertOutputContains("Invalid usage of @Setup: Method `privateMethod` is private. @Setup methods should be public.")
        }
        runner.runAndFail("jsIrBenchmark") {
            assertOutputContains("Invalid usage of @Setup: Method `privateMethod` is private. @Setup methods should be public.")
        }
        runner.runAndFail("jvmBenchmark") {
            assertOutputContains("@Setup method should be public.")
        }
    }

    @Test
    fun testTeardownWithPrivate() {
        val runner = project("kotlin-multiplatform")
        
        runner.addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
            teardown("privateMethod")
        }

        runner.runAndFail("nativeBenchmark") {
            assertOutputContains("Invalid usage of @TearDown: Method `privateMethod` is private. @TearDown methods should be public.")
        }
        runner.runAndFail("wasmJsBenchmark") {
            assertOutputContains("Invalid usage of @TearDown: Method `privateMethod` is private. @TearDown methods should be public.")
        }
        runner.runAndFail("jsIrBenchmark") {
            assertOutputContains("Invalid usage of @TearDown: Method `privateMethod` is private. @TearDown methods should be public.")
        }
        runner.runAndFail("jvmBenchmark") {
            assertOutputContains("@TearDown method should be public.")
        }
    }

    @Test
    fun testParamWithPrivate() {
        val runner = project("kotlin-multiplatform")
        
        runner.addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
            param("privateProperty", "1", "5")
        }

        runner.runAndFail("nativeBenchmark") {
            assertOutputContains("Invalid usage of @Param: Property `privateProperty` is private. Ensure properties annotated with @Param are not declared private.")
        }
        runner.runAndFail("wasmJsBenchmark") {
            assertOutputContains("Invalid usage of @Param: Property `privateProperty` is private. Ensure properties annotated with @Param are not declared private.")
        }
        runner.runAndFail("jsIrBenchmark") {
            assertOutputContains("Invalid usage of @Param: Property `privateProperty` is private. Ensure properties annotated with @Param are not declared private.")
        }
        runner.run("jvmBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
    }

    @Test
    fun testParamWithFinal() {
        val runner = project("kotlin-multiplatform")
        
        runner.addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
            param("finalProperty", "1", "5")
        }

        runner.run("nativeBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
        runner.run("wasmJsBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
        runner.run("jsIrBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
        runner.run("jvmBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
    }

    @Test
    fun testParamWithInternal() {
        val runner = project("kotlin-multiplatform")
        
        runner.addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
            param("internalProperty", "1", "5")
        }

        runner.runAndFail("nativeBenchmark") {
            assertOutputContains("Invalid usage of @Param: Property `internalProperty` is internal. Ensure properties annotated with @Param are not declared internal.")
        }
        runner.runAndFail("wasmJsBenchmark") {
            assertOutputContains("Invalid usage of @Param: Property `internalProperty` is internal. Ensure properties annotated with @Param are not declared internal.")
        }
        runner.runAndFail("jsIrBenchmark") {
            assertOutputContains("Invalid usage of @Param: Property `internalProperty` is internal. Ensure properties annotated with @Param are not declared internal.")
        }
        runner.run("jvmBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
    }
    
    @Test
    fun testSetupWithFinal() {
        val runner = project("kotlin-multiplatform")
        
        runner.addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
            setup("finalMethod")
        }

        runner.run("nativeBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
        runner.run("wasmJsBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
        runner.run("jsIrBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
        runner.run("jvmBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
    }

    @Test
    fun testSetupWithInternal() {
        val runner = project("kotlin-multiplatform")
        
        runner.addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
            setup("internalMethod")
        }

        runner.runAndFail("nativeBenchmark") {
            assertOutputContains("Invalid usage of @Setup: Method `internalMethod` is internal.")
        }
        runner.runAndFail("wasmJsBenchmark") {
            assertOutputContains("Invalid usage of @Setup: Method `internalMethod` is internal.")
        }
        runner.runAndFail("jsIrBenchmark") {
            assertOutputContains("Invalid usage of @Setup: Method `internalMethod` is internal.")
        }
        runner.run("jvmBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
    }

    @Test
    fun testTeardownWithProtected() {
        val runner = project("kotlin-multiplatform")
        
        runner.addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
            teardown("protectedMethod")
        }

        runner.runAndFail("nativeBenchmark") {
            assertOutputContains("Invalid usage of @TearDown: Method `protectedMethod` is protected.")
        }
        runner.runAndFail("wasmJsBenchmark") {
            assertOutputContains("Invalid usage of @TearDown: Method `protectedMethod` is protected.")
        }
        runner.runAndFail("jsIrBenchmark") {
            assertOutputContains("Invalid usage of @TearDown: Method `protectedMethod` is protected.")
        }
        runner.runAndFail("jvmBenchmark") {
            assertOutputContains("@TearDown method should be public.")
        }
    }

    @Test
    fun testTeardownWithFinal() {
        val runner = project("kotlin-multiplatform")
        
        runner.addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
            teardown("finalMethod")
        }

        runner.run("nativeBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
        runner.run("wasmJsBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
        runner.run("jsIrBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
        runner.run("jvmBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
    }

    @Test
    fun testTeardownWithInternal() {
        val runner = project("kotlin-multiplatform")
        
        runner.addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
            teardown("internalMethod")
        }

        runner.runAndFail("nativeBenchmark") {
            assertOutputContains("Invalid usage of @TearDown: Method `internalMethod` is internal.")
        }
        runner.runAndFail("wasmJsBenchmark") {
            assertOutputContains("Invalid usage of @TearDown: Method `internalMethod` is internal.")
        }
        runner.runAndFail("jsIrBenchmark") {
            assertOutputContains("Invalid usage of @TearDown: Method `internalMethod` is internal.")
        }
        runner.run("jvmBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
    }

    @Test
    fun testSetupWithProtected() {
        val runner = project("kotlin-multiplatform")
        
        runner.addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
            setup("protectedMethod")
        }

        runner.runAndFail("nativeBenchmark") {
            assertOutputContains("Invalid usage of @Setup: Method `protectedMethod` is protected.")
        }
        runner.runAndFail("wasmJsBenchmark") {
            assertOutputContains("Invalid usage of @Setup: Method `protectedMethod` is protected.")
        }
        runner.runAndFail("jsIrBenchmark") {
            assertOutputContains("Invalid usage of @Setup: Method `protectedMethod` is protected.")
        }
        runner.runAndFail("jvmBenchmark") {
            assertOutputContains("@Setup method should be public.")
        }
    }

    @Test
    fun testBenchmarkAndSetup() {
        val runner = project("kotlin-multiplatform")
        
        runner.addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
            benchmark("plainMethod")
            setup("plainMethod")
        }
    
        runner.run("nativeBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
        runner.run("wasmJsBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
        runner.run("jsIrBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
        runner.run("jvmBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
    }
    
    @Test
    fun testBenchmarkAndTearDown() {
        val runner = project("kotlin-multiplatform")
        
        runner.addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
            benchmark("plainMethod")
            teardown("plainMethod")
        }
    
        runner.run("nativeBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
        runner.run("wasmJsBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
        runner.run("jsIrBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
        runner.run("jvmBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
    }

    @Test
    fun testWithSetupAndTearDown() {
        val runner = project("kotlin-multiplatform")
        
        runner.addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
            setup("plainMethod")
            teardown("plainMethod")
        }

        runner.run("nativeBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
        runner.run("wasmJsBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
        runner.run("jsIrBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
        runner.run("jvmBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
    }
    
    @Test
    fun testBenchmarkWithBlackhole() {
        val runner = project("kotlin-multiplatform")

        runner.addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
            benchmark("blackholeFunction")
        }

        runner.run("nativeBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
        runner.run("wasmJsBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
        runner.run("jsIrBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
        runner.run("jvmBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
    }

    @Test
    fun testSetupWithArg() {
        val runner = project("kotlin-multiplatform")
    
        runner.addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
            setup("methodWithArg")
        }
    
        runner.runAndFail("nativeBenchmark") {
            assertOutputContains("Invalid usage of @Setup: Method `methodWithArg` has a parameter of type `Int` which is neither a Blackhole nor a @State object")
        }
        runner.runAndFail("wasmJsBenchmark") {
            assertOutputContains("Invalid usage of @Setup: Method `methodWithArg` has a parameter of type `Int` which is neither a Blackhole nor a @State object")
        }
        runner.runAndFail("jsIrBenchmark") {
            assertOutputContains("Invalid usage of @Setup: Method `methodWithArg` has a parameter of type `Int` which is neither a Blackhole nor a @State object")
        }
        runner.runAndFail("jvmBenchmark") {
            assertOutputContains("Method parameters should be either @State classes")
        }
    }

    @Test
    fun testTeardownWithArg() {
        val runner = project("kotlin-multiplatform")

        runner.addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
            teardown("methodWithArg")
        }

        runner.runAndFail("nativeBenchmark") {
            assertOutputContains("Invalid usage of @Teardown: Method `methodWithArg` has a parameter of type `Int` which is neither a Blackhole nor a @State object")
        }
        runner.runAndFail("wasmJsBenchmark") {
            assertOutputContains("Invalid usage of @Teardown: Method `methodWithArg` has a parameter of type `Int` which is neither a Blackhole nor a @State object")
        }
        runner.runAndFail("jsIrBenchmark") {
            assertOutputContains("Invalid usage of @Teardown: Method `methodWithArg` has a parameter of type `Int` which is neither a Blackhole nor a @State object")
        }
        runner.runAndFail("jvmBenchmark") {
            assertOutputContains("Method parameters should be either @State classes")
        }
    }
    
    @Test
    fun testBenchmarkWithArg() {
        val runner = project("kotlin-multiplatform")

        runner.addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
            benchmark("methodWithArg")
        }

        runner.runAndFail("nativeBenchmark") {
            assertOutputContains("Invalid usage of @Benchmark: Method `methodWithArg` has a parameter of type `Int` which is neither a Blackhole nor a @State object")
        }
        runner.runAndFail("wasmJsBenchmark") {
            assertOutputContains("Invalid usage of @Benchmark: Method `methodWithArg` has a parameter of type `Int` which is neither a Blackhole nor a @State object")
        }
        runner.runAndFail("jsIrBenchmark") {
            assertOutputContains("Invalid usage of @Benchmark: Method `methodWithArg` has a parameter of type `Int` which is neither a Blackhole nor a @State object")
        }
        runner.runAndFail("jvmBenchmark") {
            assertOutputContains("Method parameters should be either @State classes")
        }
    }

    @Test
    fun testParamWithMethod() {
        val runner = project("kotlin-multiplatform")

        runner.addAnnotation("src/commonMain/kotlin/CommonBenchmark.kt") {
            param("plainMethod")
        }

        runner.run("nativeBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
        runner.run("wasmJsBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
        runner.run("jsIrBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
        runner.run("jvmBenchmark") {
            assertOutputContains("BUILD SUCCESSFUL")
        }
    }
}
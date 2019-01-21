package org.jetbrains.gradle.benchmarks

import com.squareup.kotlinpoet.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.scopes.*
import java.io.*

enum class Platform {
    JS, NATIVE
}

class SuiteSourceGenerator(val module: ModuleDescriptor, val output: File, val platform: Platform) {
    val benchmarkAnnotationFQN = "org.jetbrains.gradle.benchmarks.Benchmark"
    val stateAnnotationFQN = "org.jetbrains.gradle.benchmarks.State"
    val mainBenchmarkPackage = "org.jetbrains.gradle.benchmarks.generated"
    val nativeSuite = ClassName.bestGuess("org.jetbrains.gradle.benchmarks.native.Suite")
    val jsSuite = ClassName.bestGuess("org.jetbrains.gradle.benchmarks.js.Suite")
    val suiteType = when (platform) {
        Platform.JS -> jsSuite
        Platform.NATIVE -> nativeSuite
    }

    val benchmarks = mutableListOf<ClassName>()


    fun generate() {
        processPackage(module, module.getPackage(FqName.ROOT)) {
            generateBenchmark(it)
        }
        generateRunnerMain()
    }

    private fun generateRunnerMain() {
        val file = FileSpec.builder(mainBenchmarkPackage, "BenchmarkSuite").apply {
            function("main") {
                addStatement("val suite = %T()", suiteType)
                for (benchmark in benchmarks) {
                    addStatement("%T().addBenchmarkToSuite(suite)", benchmark)
                }
                addStatement("suite.run()")
            }
        }.build()
        file.writeTo(output)
    }

    private fun processPackage(
        module: ModuleDescriptor,
        packageView: PackageViewDescriptor,
        process: (ClassDescriptor) -> Unit
    ) {
        for (packageFragment in packageView.fragments.filter { it.module == module }) {
            DescriptorUtils.getAllDescriptors(packageFragment.getMemberScope())
                .filterIsInstance<ClassDescriptor>()
                .filter { it.annotations.any { it.fqName.toString() == stateAnnotationFQN } }
                .forEach(process)
        }

        for (subpackageName in module.getSubPackagesOf(packageView.fqName, MemberScope.ALL_NAME_FILTER)) {
            processPackage(module, module.getPackage(subpackageName), process)
        }
    }

    private fun generateBenchmark(original: ClassDescriptor) {
        val originalPackage = original.fqNameSafe.parent()
        val originalName = original.fqNameSafe.shortName()
        val originalClass = ClassName(originalPackage.toString(), originalName.toString())

        val benchmarkPackageName = originalPackage.child(Name.identifier("generated")).toString()
        val benchmarkName = originalName.toString() + "_runner"
        val benchmarkClass = ClassName(mainBenchmarkPackage, benchmarkName)

        val markedFunctions = DescriptorUtils.getAllDescriptors(original.unsubstitutedMemberScope)
            .filterIsInstance<FunctionDescriptor>()
            .filter { it.annotations.any { it.fqName.toString() == benchmarkAnnotationFQN } }

        val file = FileSpec.builder(mainBenchmarkPackage, benchmarkName).apply {
            declareClass(benchmarkClass) {
                property("_instance", originalClass) {
                    addModifiers(KModifier.PRIVATE)
                    initializer(codeBlock {
                        addStatement("%T()", originalClass)
                    })
                }

                for (markedFunction in markedFunctions) {
                    val functionName = markedFunction.name.toString()
                    function(functionName) {
                        addStatement("_instance.%N()", functionName)
                    }
                }

                function("addBenchmarkToSuite") {
                    addParameter("suite", suiteType)
                    for (benchmark in markedFunctions) {
                        val functionName = benchmark.name.toString()
                        addStatement(
                            "suite.add(%P) { %N() }",
                            "${originalClass.canonicalName}.$functionName",
                            functionName
                        )
                    }
                }

            }
            benchmarks.add(benchmarkClass)
        }.build()

        file.writeTo(output)
    }
}

inline fun codeBlock(builderAction: CodeBlock.Builder.() -> Unit): CodeBlock {
    return CodeBlock.builder().apply(builderAction).build()
}

inline fun FileSpec.Builder.declareClass(name: String, builderAction: TypeSpec.Builder.() -> Unit): TypeSpec {
    return TypeSpec.classBuilder(name).apply(builderAction).build().also {
        addType(it)
    }
}

inline fun FileSpec.Builder.declareClass(name: ClassName, builderAction: TypeSpec.Builder.() -> Unit): TypeSpec {
    return TypeSpec.classBuilder(name).apply(builderAction).build().also {
        addType(it)
    }
}

inline fun TypeSpec.Builder.property(
    name: String,
    type: ClassName,
    builderAction: PropertySpec.Builder.() -> Unit
): PropertySpec {
    return PropertySpec.builder(name, type).apply(builderAction).build().also {
        addProperty(it)
    }
}

inline fun TypeSpec.Builder.function(
    name: String,
    builderAction: FunSpec.Builder.() -> Unit
): FunSpec {
    return FunSpec.builder(name).apply(builderAction).build().also {
        addFunction(it)
    }
}

inline fun FileSpec.Builder.function(
    name: String,
    builderAction: FunSpec.Builder.() -> Unit
): FunSpec {
    return FunSpec.builder(name).apply(builderAction).build().also {
        addFunction(it)
    }
}

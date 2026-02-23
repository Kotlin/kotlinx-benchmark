package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.SuiteSourceGenerator.Companion.blackholeClassName
import kotlinx.benchmark.gradle.SuiteSourceGenerator.Companion.paramAnnotationClassName
import kotlinx.benchmark.gradle.internal.generator.RequiresKotlinCompilerEmbeddable
import kotlinx.metadata.klib.annotations
import kotlin.metadata.KmAnnotationArgument
import kotlin.metadata.KmClassifier
import kotlin.metadata.KmFunction
import kotlin.metadata.KmProperty
import kotlin.metadata.KmType
import kotlin.metadata.Visibility
import kotlin.metadata.isVar
import kotlin.metadata.visibility

@RequiresKotlinCompilerEmbeddable
internal fun validateBenchmarkFunctions(functions: List<KmFunction>) {
    functions.forEach { function ->
        if (function.visibility != Visibility.PUBLIC) {
            error("@Benchmark function should be public. Function `${function.name}` is ${function.visibility.name.toLowerCase()}.")
        }

        val parameters = function.valueParameters.size
        if (parameters == 1) {
            val paramType = function.valueParameters[0].type
            if ((paramType.classifier as KmClassifier.Class).name != blackholeClassName) {
                error("@Benchmark function can have at most one parameter of type `Blackhole`. " +
                    "Function `${function.name}` has a parameter of type `${(paramType.classifier as KmClassifier.Class).name.replace('/', '.')}`. ")
            }
        } else if (parameters != 0) {
            error("@Benchmark function can have at most one parameter of type `Blackhole`. " +
                    "Function `${function.name}` has $parameters parameters.")
        }
    }
}

@RequiresKotlinCompilerEmbeddable
internal fun validateSetupFunctions(functions: List<KmFunction>) {
    functions.forEach { function ->
        if (function.visibility != Visibility.PUBLIC) {
            error("@Setup function should be public. Function `${function.name}` is ${function.visibility.name.toLowerCase()}.")
        }

        val parameters = function.valueParameters.size
        if (parameters != 0) {
            error("@Setup function should have no parameters. " +
                    "Function `${function.name}` has $parameters parameter${if (parameters > 1) "s" else ""}.")
        }
    }
}

@RequiresKotlinCompilerEmbeddable
internal fun validateTeardownFunctions(functions: List<KmFunction>) {
    functions.forEach { function ->
        if (function.visibility != Visibility.PUBLIC) {
            error("@TearDown function should be public. Function `${function.name}` is ${function.visibility.name.toLowerCase()}.")
        }

        val parameters = function.valueParameters.size
        if (parameters != 0) {
            error("@TearDown function should have no parameters. " +
                    "Function `${function.name}` has $parameters parameter${if (parameters > 1) "s" else ""}.")
        }
    }
}

private fun KmType.isPrimitiveOrUnsigned(): Boolean = when ((this.classifier as? KmClassifier.Class)?.name) {
    "kotlin/Boolean", "kotlin/Char", "kotlin/Byte", "kotlin/Short", "kotlin/Int", "kotlin/Float", "kotlin/Long", "kotlin/Double" -> true
    "kotlin/UByte", "kotlin/UShort", "kotlin/UInt", "kotlin/ULong" -> true
    else -> false
}

@RequiresKotlinCompilerEmbeddable
internal fun validateParameterProperties(properties: List<KmProperty>) {
    properties.forEach { property ->
        if (!property.isVar) {
            error("@Param property should be mutable (var). Property `${property.name}` is read-only (val).")
        }
        if (property.visibility != Visibility.PUBLIC) {
            error("@Param property should be public. Property `${property.name}` is ${property.visibility.name.toLowerCase()}.")
        }

        val isSupportedType = property.returnType.isPrimitiveOrUnsigned() ||
                (property.returnType.classifier as? KmClassifier.Class)?.name == "kotlin/String"
        if (!isSupportedType) {
            error("@Param property should have a primitive or string type. Property `${property.name}` type is `${(property.returnType.classifier as KmClassifier.Class).name.replace('/', '.')}`.")
        }

        val annotation = property.annotations.find { it.className == paramAnnotationClassName }!!
        val valueArgument = annotation.arguments["value"]!! as KmAnnotationArgument.ArrayValue

        if (valueArgument.elements.isEmpty()) {
            error("@Param annotation should have at least one argument. The annotation on property `${property.name}` has no arguments.")
        }
    }
}

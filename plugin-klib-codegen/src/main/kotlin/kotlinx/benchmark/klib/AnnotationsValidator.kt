package kotlinx.benchmark.klib

import kotlinx.benchmark.klib.SuiteSourceGenerator.Companion.paramAnnotationClassName
import kotlin.metadata.KmFunction
import kotlin.metadata.KmProperty
import kotlin.metadata.Visibility
import kotlin.metadata.isVar
import kotlin.metadata.visibility
import kotlinx.metadata.klib.annotations
import kotlin.metadata.KmClassifier
import kotlin.metadata.KmType

internal fun validateBenchmarkFunctions(functions: List<KmFunction>) {
    functions.forEach { function ->
        if (function.visibility != Visibility.PUBLIC) {
            error("@Benchmark function should be public. Function `${function.name}` is ${function.visibility.name.lowercase()}.")
        }

        val parameters = function.valueParameters.size
        if (parameters == 1) {
            val paramType = function.valueParameters[0].type
            if (paramType.className() != "kotlinx/benchmark/Blackhole") {
                error("@Benchmark function can have at most one parameter of type `Blackhole`. " +
                    "Function `${function.name}` has a parameter of type `${paramType.renderTypeName()}`. ")
            }
        } else if (parameters != 0) {
            error("@Benchmark function can have at most one parameter of type `Blackhole`. " +
                    "Function `${function.name}` has $parameters parameters.")
        }
    }
}

internal fun validateSetupFunctions(functions: List<KmFunction>) {
    functions.forEach { function ->
        if (function.visibility != Visibility.PUBLIC) {
            error("@Setup function should be public. Function `${function.name}` is ${function.visibility.name.lowercase()}.")
        }

        val parameters = function.valueParameters.size
        if (parameters != 0) {
            error("@Setup function should have no parameters. " +
                    "Function `${function.name}` has $parameters parameter${if (parameters > 1) "s" else ""}.")
        }
    }
}

internal fun validateTeardownFunctions(functions: List<KmFunction>) {
    functions.forEach { function ->
        if (function.visibility != Visibility.PUBLIC) {
            error("@TearDown function should be public. Function `${function.name}` is ${function.visibility.name.lowercase()}.")
        }

        val parameters = function.valueParameters.size
        if (parameters != 0) {
            error("@TearDown function should have no parameters. " +
                    "Function `${function.name}` has $parameters parameter${if (parameters > 1) "s" else ""}.")
        }
    }
}

@OptIn(KotlinxBenchmarkCodegenInternalApi::class)
internal fun validateParameterProperties(properties: List<KmProperty>) {
    properties.forEach { property ->
        if (!property.isVar) {
            error("@Param property should be mutable (var). Property `${property.name}` is read-only (val).")
        }
        if (property.visibility != Visibility.PUBLIC) {
            error("@Param property should be public. Property `${property.name}` is ${property.visibility.name.lowercase()}.")
        }
        if (!property.returnType.isPrimitiveOrString()) {
            error("@Param property should have a primitive or string type. Property `${property.name}` type is `${property.returnType.renderTypeName()}`.")
        }

        val values = property.annotations.annotation(paramAnnotationClassName)!!
            .arrayArgument("value")

        if (values.isEmpty()) {
            error("@Param annotation should have at least one argument. The annotation on property `${property.name}` has no arguments.")
        }
    }
}


private val primitiveTypes = setOf(
    "kotlin/Byte", "kotlin/Short", "kotlin/Int", "kotlin/Long",
    "kotlin/Float", "kotlin/Double",
    "kotlin/Boolean", "kotlin/Char",
    "kotlin/UByte", "kotlin/UShort", "kotlin/UInt", "kotlin/ULong"
)

private fun KmType.isPrimitiveOrString(): Boolean {
    val klass = classifier as? KmClassifier.Class ?: return false
    val name = klass.name
    return name in primitiveTypes || name == "kotlin/String"
}

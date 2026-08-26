package kotlinx.benchmark.gradle

import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.symbol.*

internal fun validateBenchmarkFunctions(functions: List<KSFunctionDeclaration>) {
    functions.forEach { function ->
        val name = function.simpleName.asString()
        val visibility = function.getVisibility()
        if (visibility != Visibility.PUBLIC) {
            error("@Benchmark function should be public. Function `$name` is ${visibility.name.lowercase()}.")
        }

        val parameters = function.parameters.size
        if (parameters == 1) {
            val paramType = function.parameters[0].type.resolve().declaration
            if (paramType.qualifiedName?.asString() != SuiteSourceGenerator.blackholeFQN) {
                error(
                    "@Benchmark function can have at most one parameter of type `Blackhole`. " +
                            "Function `$name` has a parameter of type `${paramType.simpleName.asString()}`. "
                )
            }
        } else if (parameters != 0) {
            error(
                "@Benchmark function can have at most one parameter of type `Blackhole`. " +
                        "Function `$name` has $parameters parameters."
            )
        }
    }
}

internal fun validateSetupFunctions(functions: List<KSFunctionDeclaration>) {
    functions.forEach { function ->
        val name = function.simpleName.asString()
        val visibility = function.getVisibility()
        if (visibility != Visibility.PUBLIC) {
            error("@Setup function should be public. Function `$name` is ${visibility.name.lowercase()}.")
        }

        val parameters = function.parameters.size
        if (parameters != 0) {
            error(
                "@Setup function should have no parameters. " +
                        "Function `$name` has $parameters parameter${if (parameters > 1) "s" else ""}."
            )
        }
    }
}

internal fun validateTeardownFunctions(functions: List<KSFunctionDeclaration>) {
    functions.forEach { function ->
        val name = function.simpleName.asString()
        val visibility = function.getVisibility()
        if (visibility != Visibility.PUBLIC) {
            error("@TearDown function should be public. Function `$name` is ${visibility.name.lowercase()}.")
        }

        val parameters = function.parameters.size
        if (parameters != 0) {
            error(
                "@TearDown function should have no parameters. " +
                        "Function `$name` has $parameters parameter${if (parameters > 1) "s" else ""}."
            )
        }
    }
}

internal fun validateParameterProperties(properties: List<KSPropertyDeclaration>) {
    properties.forEach { property ->
        val name = property.simpleName.asString()
        if (!property.isMutable) {
            error("@Param property should be mutable (var). Property `$name` is read-only (val).")
        }
        val visibility = property.getVisibility()
        if (visibility != Visibility.PUBLIC) {
            error("@Param property should be public. Property `$name` is ${visibility.name.lowercase()}.")
        }
        val type = property.type.resolve().declaration
        val supportedBuiltinTypes = setOf(
            "kotlin.Boolean",
            "kotlin.Char",
            "kotlin.Byte",
            "kotlin.Short",
            "kotlin.Int",
            "kotlin.Float",
            "kotlin.Long",
            "kotlin.Double",
            "kotlin.UByte",
            "kotlin.UShort",
            "kotlin.UInt",
            "kotlin.ULong",
            "kotlin.String",
        )
        if (type.qualifiedName?.asString() !in supportedBuiltinTypes) {
            error("@Param property should have a primitive or string type. Property `$name` type is `${type.simpleName.asString()}`.")
        }

        val annotation = property.annotationOrNull(SuiteSourceGenerator.paramAnnotationFQN)!!
        val values = annotation.argumentValueOrNull<List<*>>("value")
        if (values?.isEmpty() != false) {
            error("@Param annotation should have at least one argument. The annotation on property `$name` has no arguments.")
        }
    }
}

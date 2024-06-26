package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.SuiteSourceGenerator.Companion.paramAnnotationFQN
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.UnsignedTypes
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.js.descriptorUtils.getKotlinTypeFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.annotations.argumentValue

internal fun validateBenchmarkFunctions(functions: List<FunctionDescriptor>) {
    functions.forEach { function ->
        if (function.visibility != DescriptorVisibilities.PUBLIC) {
            error("@Benchmark function should be public. Function `${function.name}` is ${function.visibility.name}.")
        }

        val parameters = function.valueParameters.size
        if (parameters == 1) {
            val paramType = function.valueParameters[0].type
            if (paramType.getKotlinTypeFqName(false) != "kotlinx.benchmark.Blackhole") {
                error("@Benchmark function can have at most one parameter of type `Blackhole`. " +
                    "Function `${function.name}` has a parameter of type `$paramType`. ")
            }
        } else if (parameters != 0) {
            error("@Benchmark function can have at most one parameter of type `Blackhole`. " +
                    "Function `${function.name}` has $parameters parameters.")
        }
    }
}

internal fun validateSetupFunctions(functions: List<FunctionDescriptor>) {
    functions.forEach { function ->
        if (function.visibility != DescriptorVisibilities.PUBLIC) {
            error("@Setup function should be public. Function `${function.name}` is ${function.visibility.name}.")
        }

        val parameters = function.valueParameters.size
        if (parameters != 0) {
            error("@Setup function should have no parameters. " +
                    "Function `${function.name}` has $parameters parameter${if (parameters > 1) "s" else ""}.")
        }
    }
}

internal fun validateTeardownFunctions(functions: List<FunctionDescriptor>) {
    functions.forEach { function ->
        if (function.visibility != DescriptorVisibilities.PUBLIC) {
            error("@TearDown function should be public. Function `${function.name}` is ${function.visibility.name}.")
        }

        val parameters = function.valueParameters.size
        if (parameters != 0) {
            error("@TearDown function should have no parameters. " +
                    "Function `${function.name}` has $parameters parameter${if (parameters > 1) "s" else ""}.")
        }
    }
}

internal fun validateParameterProperties(properties: List<PropertyDescriptor>) {
    properties.forEach { property ->
        if (!property.isVar) {
            error("@Param property should be mutable (var). Property `${property.name}` is read-only (val).")
        }
        if (property.visibility != DescriptorVisibilities.PUBLIC) {
            error("@Param property should be public. Property `${property.name}` is ${property.visibility.name}.")
        }
        val isSupportedType = KotlinBuiltIns.isPrimitiveTypeOrNullablePrimitiveType(property.type) ||
                UnsignedTypes.isUnsignedType(property.type) ||
                property.type.getKotlinTypeFqName(false) == "kotlin.String"
        if (!isSupportedType) {
            error("@Param property should have a primitive or string type. Property `${property.name}` type is `${property.type}`.")
        }

        val annotation = property.annotations.findAnnotation(FqName(paramAnnotationFQN))!!
        val valueArgument = annotation.argumentValue("value")!!
        val values = valueArgument.value as List<*>

        if (values.isEmpty()) {
            error("@Param annotation should have at least one argument. The annotation on property `${property.name}` has no arguments.")
        }
    }
}
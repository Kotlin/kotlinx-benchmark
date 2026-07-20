package kotlinx.benchmark.gradle

import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Visibility
import kotlinx.benchmark.gradle.SuiteSourceGenerator.Companion.paramAnnotationFQN
import kotlinx.benchmark.gradle.internal.generator.RequiresKotlinCompilerEmbeddable
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.UnsignedTypes
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.js.descriptorUtils.getKotlinTypeFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.annotations.argumentValue

@RequiresKotlinCompilerEmbeddable
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

@JvmName("validateBenchmarkFunctionsKSP")
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
            if (paramType.qualifiedName?.asString() != "kotlinx.benchmark.Blackhole") {
                error("@Benchmark function can have at most one parameter of type `Blackhole`. " +
                        "Function `$name` has a parameter of type `${paramType.simpleName.asString()}`. ")
            }
        } else if (parameters != 0) {
            error("@Benchmark function can have at most one parameter of type `Blackhole`. " +
                    "Function `$name` has $parameters parameters.")
        }
    }
}

@RequiresKotlinCompilerEmbeddable
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

@JvmName("validateSetupFunctionsKSP")
internal fun validateSetupFunctions(functions: List<KSFunctionDeclaration>) {
    functions.forEach { function ->
        val name = function.simpleName.asString()
        val visibility = function.getVisibility()
        if (visibility != Visibility.PUBLIC) {
            error("@Setup function should be public. Function `$name` is ${visibility.name.lowercase()}.")
        }

        val parameters = function.parameters.size
        if (parameters != 0) {
            error("@Setup function should have no parameters. " +
                    "Function `$name` has $parameters parameter${if (parameters > 1) "s" else ""}.")
        }
    }
}

@RequiresKotlinCompilerEmbeddable
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

@JvmName("validateTeardownFunctionsKSP")
internal fun validateTeardownFunctions(functions: List<KSFunctionDeclaration>) {
    functions.forEach { function ->
        val name = function.simpleName.asString()
        val visibility = function.getVisibility()
        if (visibility != Visibility.PUBLIC) {
            error("@TearDown function should be public. Function `$name` is ${visibility.name.lowercase()}.")
        }

        val parameters = function.parameters.size
        if (parameters != 0) {
            error("@TearDown function should have no parameters. " +
                    "Function `$name` has $parameters parameter${if (parameters > 1) "s" else ""}.")
        }
    }
}

@RequiresKotlinCompilerEmbeddable
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

@JvmName("validateParameterPropertiesKSP")
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
        val supportedBuiltinTypes = listOf(
            Boolean::class,
            Char::class,
            Byte::class,
            Short::class,
            Int::class,
            Float::class,
            Long::class,
            Double::class,
            UByte::class,
            UShort::class,
            UInt::class,
            ULong::class,
            String::class,
        ).map { it.qualifiedName!! }.toSet()
        val isSupportedType = supportedBuiltinTypes.contains(type.qualifiedName?.asString())
        if (!isSupportedType) {
            error("@Param property should have a primitive or string type. Property `$name` type is `${type.simpleName.asString()}`.")
        }

        val annotation = property.annotationOrNull(SuiteSourceGeneratorWithKSP.paramAnnotationFQN)!!

        val values = annotation.argumentValueOrNull<List<*>>("value")

        if (values?.isEmpty() != false) {
            error("@Param annotation should have at least one argument. The annotation on property `$name` has no arguments.")
        }
    }
}

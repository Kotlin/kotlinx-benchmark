package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.SuiteSourceGenerator.Companion.setupAnnotationFQN
import kotlinx.benchmark.gradle.SuiteSourceGenerator.Companion.teardownAnnotationFQN
import kotlinx.benchmark.gradle.internal.generator.*
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.tree.*
import org.slf4j.*
import java.util.*
import java.util.jar.*

/**
 * File with classes used to run annotation processing on benchmarks written for Android, using
 * the kotlinx-benchmark API's.
 */

internal data class AnnotationData(
    val name: String,
    val parameters: Map<String, Any?>
)

internal data class ClassAnnotationsDescriptor(
    val packageName: String,
    val name: String,
    val visibility: Visibility,
    val isAbstract: Boolean,
    val annotations: List<AnnotationData>,
    val methods: List<MethodAnnotationsDescriptor>,
    val fields: List<FieldAnnotationsDescriptor>
)

internal data class MethodAnnotationsDescriptor(
    val name: String,
    val visibility: Visibility,
    val annotations: List<AnnotationData>,
    val parameters: List<String>
)

internal data class FieldAnnotationsDescriptor(
    val name: String,
    val visibility: Visibility,
    val annotations: List<AnnotationData>,
    val type: String
)

internal enum class Visibility {
    PUBLIC, PROTECTED, PRIVATE, PACKAGE_PRIVATE
}

internal class AnnotationProcessor {

    private val classAnnotationsDescriptors = mutableListOf<ClassAnnotationsDescriptor>()

    fun processJarFile(jarFile: JarFile, logger: Logger) {
        val entries = jarFile.entries()

        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            if (entry.name.endsWith(".class")) {
                val inputStream = jarFile.getInputStream(entry)
                val classBytes = inputStream.readBytes()
                processClassBytes(classBytes, logger)
            }
        }
    }

    private fun processClassBytes(classBytes: ByteArray, logger: Logger) {
        val classReader = ClassReader(classBytes)
        val classNode = ClassNode()
        classReader.accept(classNode, 0)

        val classAnnotations = classNode.visibleAnnotations
            ?.filter { it.desc != "Lkotlin/Metadata;" }
            ?.map { parseAnnotation(it) }
            ?: emptyList()

        val methodAnnotationsMap: Map<String, List<AnnotationData>> = classNode.methods
            .filterNot { it.name == "<init>" }
            .filter { methodNode ->
                methodNode.name.matches(Regex("^get[A-Z].*\\\$annotations\$")) &&
                        methodNode.visibleAnnotations?.isNotEmpty() == true
            }
            .associate { methodNode ->
                val fieldName = methodNode.name
                    .removePrefix("get")
                    .removeSuffix("\$annotations")
                    .replaceFirstChar { it.lowercase(Locale.ROOT) }

                val methodAnnotations = methodNode.visibleAnnotations
                    ?.filter { it.desc != "Lkotlin/Metadata;" }
                    ?.map { parseAnnotation(it) }
                    ?: emptyList()

                fieldName to methodAnnotations
            }

        val methodDescriptors = classNode.methods
            .filter { methodNode ->
                methodNode.visibleAnnotations?.any { it.desc != "Lkotlin/Metadata;" } == true
            }
            .filterNot { it.name == "<init>" }
            .filterNot { it.name.matches(Regex("^get[A-Z].*\\\$annotations\$")) }
            .map { methodNode ->
                val methodAnnotations = methodNode.visibleAnnotations
                    ?.filter { it.desc != "Lkotlin/Metadata;" }
                    ?.map { parseAnnotation(it) }
                    ?: emptyList()
                val parameters = Type.getArgumentTypes(methodNode.desc).map { it.className }
                MethodAnnotationsDescriptor(
                    methodNode.name,
                    getVisibility(methodNode.access),
                    methodAnnotations,
                    parameters
                )
            }

        val fieldDescriptors = classNode.fields.map { fieldNode ->
            val fieldAnnotations = methodAnnotationsMap.getOrDefault(fieldNode.name, emptyList())
            val fieldType = Type.getType(fieldNode.desc).className

            FieldAnnotationsDescriptor(
                fieldNode.name,
                getFieldVisibility(classNode, fieldNode),
                fieldAnnotations,
                fieldType
            )
        }

        val packageName = classNode.name.substringBeforeLast('/', "").replace('/', '.')
        val className = classNode.name.substringAfterLast('/')
        val classDescriptor = ClassAnnotationsDescriptor(
            packageName,
            className,
            getVisibility(classNode.access),
            isAbstract(classNode.access),
            classAnnotations,
            methodDescriptors,
            fieldDescriptors
        )

        classAnnotationsDescriptors.add(classDescriptor)
        logger.debug("Processed class: ${classDescriptor.name}")
    }

    private fun parseAnnotation(annotationNode: AnnotationNode): AnnotationData {
        val parameters = mutableMapOf<String, Any?>()
        annotationNode.values?.let { values ->
            for (i in values.indices step 2) {
                val name = values[i] as String
                val value = values[i + 1]
                parameters[name] = formatAnnotationValue(value)
            }
        }
        return AnnotationData(formatDescriptor(annotationNode.desc), parameters)
    }

    private fun formatAnnotationValue(value: Any?): Any? {
        return when (value) {
            is List<*> -> value.flatMap {
                val formattedValue = formatAnnotationValue(it)
                if (formattedValue is List<*>) {
                    formattedValue
                } else {
                    listOf(formattedValue)
                }
            }
            is Array<*> -> value.flatMap {
                val formattedValue = formatAnnotationValue(it)
                if (formattedValue is List<*>) {
                    formattedValue
                } else {
                    listOf(formattedValue)
                }
            }
            is TypePath -> value.toString()
            is AnnotationNode -> formatAnnotationNode(value)
            is Type -> value.className.replace('/', '.')
            is String -> {
                if (value.startsWith("L") && value.endsWith(";")) {
                    formatDescriptor(value)
                } else {
                    value
                }
            }
            is ByteArray -> value.toList()
            is CharArray -> value.map { it.toString() }
            is ShortArray -> value.toList()
            is IntArray -> value.toList()
            is LongArray -> value.toList()
            is FloatArray -> value.toList()
            is DoubleArray -> value.toList()
            is BooleanArray -> value.toList()
            else -> value
        }
    }

    private fun formatAnnotationNode(annotationNode: AnnotationNode): String {
        val sb = StringBuilder("@${formatDescriptor(annotationNode.desc)}(")
        annotationNode.values?.let { values ->
            for (i in values.indices step 2) {
                val name = values[i]
                val value = values[i + 1]
                sb.append("$name = ${formatAnnotationValue(value)}, ")
            }
        }
        if (sb.endsWith(", ")) {
            sb.setLength(sb.length - 2)
        }
        sb.append(")")
        return sb.toString()
    }

    private fun formatDescriptor(descriptor: String): String {
        return descriptor.removePrefix("L").removeSuffix(";").replace('/', '.')
    }

    private fun getVisibility(access: Int): Visibility {
        return when {
            (access and Opcodes.ACC_PUBLIC) != 0 -> Visibility.PUBLIC
            (access and Opcodes.ACC_PROTECTED) != 0 -> Visibility.PROTECTED
            (access and Opcodes.ACC_PRIVATE) != 0 -> Visibility.PRIVATE
            else -> Visibility.PACKAGE_PRIVATE
        }
    }

    private fun getFieldVisibility(classNode: ClassNode, fieldNode: FieldNode): Visibility {
        val getterName = "get${fieldNode.name.replaceFirstChar { it.titlecase(Locale.ROOT) }}"
        val setterName = "set${fieldNode.name.replaceFirstChar { it.titlecase(Locale.ROOT) }}"

        val getterMethod = classNode.methods.find { it.name == getterName }
        val setterMethod = classNode.methods.find { it.name == setterName }

        val getterVisibility = getterMethod?.let { getVisibility(it.access) }
        val setterVisibility = setterMethod?.let { getVisibility(it.access) }

        return when {
            getterVisibility != null && setterVisibility != null -> {
                if (getterVisibility == setterVisibility) {
                    getterVisibility
                } else {
                    getVisibility(fieldNode.access)
                }
            }
            getterVisibility != null -> getterVisibility
            setterVisibility != null -> setterVisibility
            else -> getVisibility(fieldNode.access)
        }
    }

    private fun isAbstract(access: Int): Boolean {
        return (access and Opcodes.ACC_ABSTRACT) != 0
    }

    fun getClassDescriptors(): List<ClassAnnotationsDescriptor> {
        return classAnnotationsDescriptors
    }
}

internal fun ClassAnnotationsDescriptor.getSpecificField(annotationName: String): List<FieldAnnotationsDescriptor> {
    return fields.filter { field ->
        field.annotations.any { it.name == annotationName }
    }
}

@OptIn(RequiresKotlinCompilerEmbeddable::class)
internal fun ClassAnnotationsDescriptor.hasSetupOrTeardownMethods(): Boolean {
    return methods.any { method ->
        method.annotations.any { it.name == setupAnnotationFQN || it.name == teardownAnnotationFQN }
    }
}
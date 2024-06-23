package kotlinx.benchmark.gradle

import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.tree.*

data class AnnotationData(
    val name: String,
    val parameters: Map<String, Any?>
)

data class ClassAnnotationsDescriptor(
    val packageName: String,
    val name: String,
    val visibility: String,
    val annotations: List<AnnotationData>,
    val methods: List<MethodAnnotationsDescriptor>,
    val fields: List<FieldAnnotationsDescriptor>
)

data class MethodAnnotationsDescriptor(
    val name: String,
    val visibility: String,
    val annotations: List<AnnotationData>
)

data class FieldAnnotationsDescriptor(
    val name: String,
    val visibility: String,
    val annotations: List<AnnotationData>
)

class AnnotationProcessor {

    private val classAnnotationsDescriptors = mutableListOf<ClassAnnotationsDescriptor>()

    fun processClassBytes(classBytes: ByteArray) {
        val classReader = ClassReader(classBytes)
        val classNode = ClassNode()
        classReader.accept(classNode, 0)

        val classAnnotations = classNode.visibleAnnotations
            ?.filter { it.desc != "Lkotlin/Metadata;" }
            ?.map { parseAnnotation(it) }
            ?: emptyList()

        val methodDescriptors = classNode.methods.map { methodNode ->
            val methodAnnotations = methodNode.visibleAnnotations
                ?.filter { it.desc != "Lkotlin/Metadata;" }
                ?.map { parseAnnotation(it) }
                ?: emptyList()
            MethodAnnotationsDescriptor(methodNode.name, getVisibility(methodNode.access), methodAnnotations)
        }

        val fieldDescriptors = classNode.fields.map { fieldNode ->
            val fieldAnnotations = fieldNode.visibleAnnotations
                ?.filter { it.desc != "Lkotlin/Metadata;" }
                ?.map { parseAnnotation(it) }
                ?: emptyList()
            FieldAnnotationsDescriptor(fieldNode.name, getFieldVisibility(classNode, fieldNode), fieldAnnotations)
        }

        val packageName = classNode.name.substringBeforeLast('/', "").replace('/', '.')
        val classDescriptor = ClassAnnotationsDescriptor(
            packageName,
            classNode.name.replace('/', '.').substringAfterLast('/'),
            getVisibility(classNode.access),
            classAnnotations,
            methodDescriptors,
            fieldDescriptors
        )

        classAnnotationsDescriptors.add(classDescriptor)
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
        return AnnotationData(annotationNode.desc.removePrefix("L").removeSuffix(";").replace('/', '.'), parameters)
    }

    private fun formatAnnotationValue(value: Any?): Any? {
        return when (value) {
            is List<*> -> value.map { formatAnnotationValue(it) }
            is Array<*> -> value.map { formatAnnotationValue(it) }
            is TypePath -> value.toString()
            is AnnotationNode -> formatAnnotationNode(value)
            is Type -> value.className
            is String -> value.replace("\"", "\\\"")
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
        val sb = StringBuilder("@${annotationNode.desc}(")
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

    private fun getVisibility(access: Int): String {
        return when {
            (access and Opcodes.ACC_PUBLIC) != 0 -> "public"
            (access and Opcodes.ACC_PROTECTED) != 0 -> "protected"
            (access and Opcodes.ACC_PRIVATE) != 0 -> "private"
            else -> "package-private"
        }
    }

    private fun getFieldVisibility(classNode: ClassNode, fieldNode: FieldNode): String {
        val getterName = "get${fieldNode.name.capitalize()}"
        val setterName = "set${fieldNode.name.capitalize()}"

        val getterMethod = classNode.methods.find { it.name == getterName }
        val setterMethod = classNode.methods.find { it.name == setterName }

        return if (getterMethod != null && setterMethod != null) {
            val getterVisibility = getVisibility(getterMethod.access)
            val setterVisibility = getVisibility(setterMethod.access)
            if (getterVisibility == setterVisibility) getterVisibility else "package-private"
        } else {
            getVisibility(fieldNode.access)
        }
    }

    fun getClassDescriptors(): List<ClassAnnotationsDescriptor> {
        return classAnnotationsDescriptors
    }

    fun getClassAnnotations(packageName: String, className: String): List<AnnotationData> {
        return classAnnotationsDescriptors
            .filter { it.packageName == packageName && it.name == className }
            .flatMap { it.annotations }
    }
}
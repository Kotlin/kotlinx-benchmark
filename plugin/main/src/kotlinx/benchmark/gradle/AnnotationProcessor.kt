package kotlinx.benchmark.gradle

import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.tree.*
import java.util.jar.*

data class AnnotationData(
    val name: String,
    val parameters: Map<String, Any?>
)

data class ClassAnnotationsDescriptor(
    val packageName: String,
    val name: String,
    val visibility: Visibility,
    val isAbstract: Boolean,
    val annotations: List<AnnotationData>,
    val methods: List<MethodAnnotationsDescriptor>,
    val fields: List<FieldAnnotationsDescriptor>
)

data class MethodAnnotationsDescriptor(
    val name: String,
    val visibility: Visibility,
    val annotations: List<AnnotationData>
)

data class FieldAnnotationsDescriptor(
    val name: String,
    val visibility: Visibility,
    val annotations: List<AnnotationData>
)

enum class Visibility {
    PUBLIC, PROTECTED, PRIVATE, PACKAGE_PRIVATE
}

class AnnotationProcessor {

    private val classAnnotationsDescriptors = mutableListOf<ClassAnnotationsDescriptor>()

    fun processJarFile(jarFile: JarFile) {
        val entries = jarFile.entries()

        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            if (entry.name.endsWith(".class")) {
                val inputStream = jarFile.getInputStream(entry)
                val classBytes = inputStream.readBytes()
                processClassBytes(classBytes)
            }
        }
    }

    private fun processClassBytes(classBytes: ByteArray) {
        val classReader = ClassReader(classBytes)
        val classNode = ClassNode()
        classReader.accept(classNode, 0)

        val classAnnotations = classNode.visibleAnnotations
            ?.filter { it.desc != "Lkotlin/Metadata;" }
            ?.map { parseAnnotation(it) }
            ?: emptyList()

        val methodDescriptors = classNode.methods
            .filterNot { it.name.startsWith("get") || it.name.startsWith("set") || it.name == "<init>" }
            .map { methodNode ->
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

    private fun getVisibility(access: Int): Visibility {
        return when {
            (access and Opcodes.ACC_PUBLIC) != 0 -> Visibility.PUBLIC
            (access and Opcodes.ACC_PROTECTED) != 0 -> Visibility.PROTECTED
            (access and Opcodes.ACC_PRIVATE) != 0 -> Visibility.PRIVATE
            else -> Visibility.PACKAGE_PRIVATE
        }
    }

    private fun getFieldVisibility(classNode: ClassNode, fieldNode: FieldNode): Visibility {
        val getterName = "get${fieldNode.name.capitalize()}"
        val setterName = "set${fieldNode.name.capitalize()}"

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

    fun getMethodDescriptors(className: String): List<MethodAnnotationsDescriptor> {
        return classAnnotationsDescriptors.find { it.name == className }?.methods ?: emptyList()
    }

    fun getFieldDescriptors(className: String): List<FieldAnnotationsDescriptor> {
        return classAnnotationsDescriptors.find { it.name == className }?.fields ?: emptyList()
    }
}
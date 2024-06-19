package kotlinx.benchmark.gradle

import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.tree.*
import java.io.File

data class AnnotationData(
    val parameters: Map<String, Any?>
)

class AnnotationProcessor {

    private val classAnnotations = mutableMapOf<String, MutableMap<String, AnnotationData>>()

    fun processClassFile(classFile: File) {
        val classReader = ClassReader(classFile.readBytes())
        val classNode = ClassNode()
        classReader.accept(classNode, 0)

        val annotations = mutableMapOf<String, AnnotationData>()

        classNode.visibleAnnotations?.forEach { annotationNode ->
            if (annotationNode.desc != "Lkotlin/Metadata;") {
                val annotationData = parseAnnotation(annotationNode)
                annotations[annotationNode.desc] = annotationData
            }
        }

        classNode.methods?.forEach { methodNode ->
            methodNode.visibleAnnotations?.forEach { annotationNode ->
                if (annotationNode.desc != "Lkotlin/Metadata;") {
                    val annotationData = parseAnnotation(annotationNode)
                    annotations[annotationNode.desc] = annotationData
                }
            }
        }

        classNode.fields?.forEach { fieldNode ->
            fieldNode.visibleAnnotations?.forEach { annotationNode ->
                if (annotationNode.desc != "Lkotlin/Metadata;") {
                    val annotationData = parseAnnotation(annotationNode)
                    annotations[annotationNode.desc] = annotationData
                }
            }
        }

        classAnnotations[classNode.name] = annotations
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
        return AnnotationData(parameters)
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

    fun getClassAnnotations(): Map<String, Map<String, AnnotationData>> {
        return classAnnotations
    }
}
package kotlinx.benchmark.gradle

import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.tree.*
import java.io.File

class AnnotationProcessor {

    fun processClassFile(classFile: File) {
        val classReader = ClassReader(classFile.readBytes())
        val classNode = ClassNode()
        classReader.accept(classNode, 0)

        classNode.visibleAnnotations?.forEach { annotationNode ->
            println("Class annotation: ${annotationNode.desc}")
            if (annotationNode.desc != "Lkotlin/Metadata;") {
                printAnnotationValues(annotationNode)
            } else {
                println("Ignoring kotlin.Metadata annotation for readability.")
            }
        }

        classNode.methods?.forEach { methodNode ->
            methodNode.visibleAnnotations?.forEach { annotationNode ->
                println("Method annotation in ${methodNode.name}: ${annotationNode.desc}")
                if (annotationNode.desc != "Lkotlin/Metadata;") {
                    printAnnotationValues(annotationNode)
                } else {
                    println("Ignoring kotlin.Metadata annotation for readability.")
                }
            }
        }

        classNode.fields?.forEach { fieldNode ->
            fieldNode.visibleAnnotations?.forEach { annotationNode ->
                println("Field annotation in ${fieldNode.name}: ${annotationNode.desc}")
                if (annotationNode.desc != "Lkotlin/Metadata;") {
                    printAnnotationValues(annotationNode)
                } else {
                    println("Ignoring kotlin.Metadata annotation for readability.")
                }
            }
        }
    }

    private fun printAnnotationValues(annotationNode: AnnotationNode) {
        annotationNode.values?.let { values ->
            for (i in values.indices step 2) {
                val name = values[i]
                val value = values[i + 1]
                println("Annotation parameter: $name = ${formatAnnotationValue(value)}")
            }
        }
    }

    private fun formatAnnotationValue(value: Any?): String {
        return when (value) {
            is List<*> -> value.joinToString(prefix = "[", postfix = "]") { formatAnnotationValue(it) }
            is Array<*> -> value.joinToString(prefix = "[", postfix = "]") { formatAnnotationValue(it) }
            is TypePath -> value.toString()
            is AnnotationNode -> formatAnnotationNode(value)
            is Type -> value.className
            is String -> "\"${value.replace("\"", "\\\"")}\""
            is ByteArray -> value.joinToString(prefix = "[", postfix = "]") { it.toString() }
            is CharArray -> value.joinToString(prefix = "[", postfix = "]") { "\"${it}\"" }
            is ShortArray -> value.joinToString(prefix = "[", postfix = "]") { it.toString() }
            is IntArray -> value.joinToString(prefix = "[", postfix = "]") { it.toString() }
            is LongArray -> value.joinToString(prefix = "[", postfix = "]") { it.toString() }
            is FloatArray -> value.joinToString(prefix = "[", postfix = "]") { it.toString() }
            is DoubleArray -> value.joinToString(prefix = "[", postfix = "]") { it.toString() }
            is BooleanArray -> value.joinToString(prefix = "[", postfix = "]") { it.toString() }
            else -> value.toString()
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
}
package kotlinx.benchmark.integration

@OptIn(ExperimentalStdlibApi::class)
class AnnotationsSpecifier {
    private val classAnnotations = mutableListOf<Annotation>()
    private val propertyAnnotations = mutableListOf<AnnotatedMember>()
    private val functionAnnotations = mutableListOf<AnnotatedMember>()

    fun measurement(iterations: Int, time: Int, timeUnit: String) {
        classAnnotations.add(
            Annotation("@Measurement", listOf(iterations, time, timeUnit))
        )
    }

    fun warmup(iterations: Int, time: Int, timeUnit: String) {
        classAnnotations.add(
            Annotation("@Warmup", listOf(iterations, time, timeUnit))
        )
    }

    fun outputTimeUnit(timeUnit: String) {
        classAnnotations.add(
            Annotation("@OutputTimeUnit", listOf(timeUnit))
        )
    }

    fun benchmarkMode(mode: String) {
        classAnnotations.add(
            Annotation("@BenchmarkMode", listOf(mode))
        )
    }

    fun benchmark(functionName: String) {
        functionAnnotations.add(
            AnnotatedMember(functionName, Annotation("@Benchmark"))
        )
    }

    fun setup(functionName: String) {
        functionAnnotations.add(
            AnnotatedMember(functionName, Annotation("@Setup"))
        )
    }

    fun teardown(functionName: String) {
        functionAnnotations.add(
            AnnotatedMember(functionName, Annotation("@TearDown"))
        )
    }

    fun param(propertyName: String, vararg values: String) {
        require(values.all { '\"' !in it }) { "TODO: Support param values that contain '\"'." }

        propertyAnnotations.add(
            AnnotatedMember(propertyName, Annotation("@Param", values.map { "\"$it\"" }))
        )
    }

    fun annotationsForProperty(line: String): List<String> {
        val annotations = mutableListOf<String>()
        for ((propertyName, annotation) in propertyAnnotations) {
            val regex = Regex("\\s*(public|private|protected|internal)?\\s*(final|open)?\\s*(val|var)\\s+${Regex.escape(propertyName)}")
            if (regex.matchesAt(line, 0)) {
                check(!annotation.isUsed)
                annotation.isUsed = true
                annotations.add(annotation.toCode())
            }
        }
        return annotations
    }
    
    fun annotationsForFunction(line: String): List<String> {
        val annotations = mutableListOf<String>()
        for ((functionName, annotation) in functionAnnotations) {
            val regex = Regex("\\s*(public|private|protected|internal)?\\s*(final|open)?\\s*fun\\s+${Regex.escape(functionName)}\\(")
            if (regex.matchesAt(line, 0)) {
                check(!annotation.isUsed)
                annotation.isUsed = true
                annotations.add(annotation.toCode())
            }
        }
        return annotations
    }

    fun replaceClassAnnotation(line: String): String {
        val trimmedLine = line.trimStart()
        val prefix = line.substring(0, line.length - trimmedLine.length)
        for (annotation in classAnnotations) {
            if (trimmedLine.startsWith(annotation.name)) {
                check(!annotation.isUsed)
                annotation.isUsed = true
                return prefix + annotation.toCode()
            }
        }
        return line
    }

    fun checkAllAnnotationsAreUsed() {
        classAnnotations.forEach { check(it.isUsed) { "Unused class annotation: $it" } }
        propertyAnnotations.forEach { check(it.annotation.isUsed) { "Unused property annotation: $it" } }
        functionAnnotations.forEach { check(it.annotation.isUsed) { "Unused function annotation: $it" } }
    }
}

private data class AnnotatedMember(
    val memberName: String,
    val annotation: Annotation
)

private data class Annotation(
    val name: String,
    val arguments: List<Any?> = emptyList(),
    var isUsed: Boolean = false
) {
    fun toCode(): String =
        "$name${if (arguments.isEmpty()) "" else arguments.joinToString(", ", "(", ")")}"
}
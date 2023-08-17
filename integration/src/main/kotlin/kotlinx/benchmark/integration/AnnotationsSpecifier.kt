package kotlinx.benchmark.integration

class AnnotationsSpecifier {

    private var isMeasurementSpecified: Boolean = false
    private var isOutputTimeUnitSpecified: Boolean = false
    private var isBenchmarkModeSpecified: Boolean = false
    private var iterations: Int? = null
    private var time: Int? = null
    private var timeUnit: String? = null
    private var outputTimeUnit: String? = null
    private var benchmarkMode: String? = null

    private var fieldName: String = ""
    private val fieldAnnotations = mutableListOf<String>()

    private var methodName: String = ""
    private val methodAnnotations = mutableListOf<String>()

    fun measurement(iterations: Int, time: Int, timeUnit: String) {
        isMeasurementSpecified = true
        this.iterations = iterations
        this.time = time
        this.timeUnit = timeUnit
    }

    fun outputTimeUnit(timeUnit: String) {
        isOutputTimeUnitSpecified = true
        this.outputTimeUnit = timeUnit
    }

    fun benchmarkMode(mode: String) {
        isBenchmarkModeSpecified = true
        this.benchmarkMode = mode
    }

    fun setup(methodName: String) {
        this.methodName = methodName
        methodAnnotations.add("@Setup")
    }

    fun teardown(methodName: String) {
        this.methodName = methodName
        methodAnnotations.add("@TearDown")
    }

    fun param(fieldName: String, vararg values: String) {
        this.fieldName = fieldName
        fieldAnnotations.add("@Param(${values.joinToString(", ") { "\"$it\"" }})")
    }

    fun getAnnotationsForField(line: String): String? {
        if (line.trimStart().startsWith("var $fieldName ")) {
            return fieldAnnotations.joinToString("\n")
        }
        return null
    }

    fun getAnnotationsForMethod(line: String): String? {
        if (line.contains("fun $methodName(")) {
            return methodAnnotations.joinToString("\n")
        }
        return null
    }

    fun replacementForLine(line: String): String {
        val trimmedLine = line.trimStart()
        val prefix = line.substring(0, line.length - trimmedLine.length)
        return when {
            isMeasurementSpecified && trimmedLine.startsWith("@Measurement") ->
                "$prefix@Measurement($iterations, $time, $timeUnit)"
            isOutputTimeUnitSpecified && trimmedLine.startsWith("@OutputTimeUnit") ->
                "$prefix@OutputTimeUnit($outputTimeUnit)"
            isBenchmarkModeSpecified && trimmedLine.startsWith("@BenchmarkMode") ->
                "$prefix@BenchmarkMode($benchmarkMode)"
            else -> line
        }
    }
}

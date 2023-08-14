package kotlinx.benchmark.integration

class AnnotationsSpecifier {
    private var isMeasurementSpecified: Boolean = false
    private var isOutputTimeUnitSpecified: Boolean = false
    private var iterations: Int? = null
    private var time: Int? = null
    private var timeUnit: String? = null
    private var outputTimeUnit: String? = null

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

    fun replacementForLine(line: String): String {
        val trimmedLine = line.trimStart()
        val prefix = line.substring(0, line.length - trimmedLine.length)
        return when {
            isMeasurementSpecified && trimmedLine.startsWith("@Measurement") ->
                "$prefix@Measurement($iterations, $time, $timeUnit)"
            isOutputTimeUnitSpecified && trimmedLine.startsWith("@OutputTimeUnit") ->
                "$prefix@OutputTimeUnit($outputTimeUnit)"
            else ->
                line
        }
    }
}

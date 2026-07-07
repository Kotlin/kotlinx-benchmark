package kotlinx.benchmark

internal expect fun String.readFile(): String

internal expect fun String.writeFile(text: String)

internal enum class StartMode {
    Default,
    StartAll,
    RunSingleWithOutputSplitter,
    RunSingle
}

internal data class StartArguments(
    val config: String,
    val mode: StartMode,
    val suiteId: String? = null,
    val benchmarkId: String? = null
) {
    companion object {
        fun parse(arguments: Array<out String>): StartArguments {
            val config: String = arguments[0]
            if (arguments.size == 1) return StartArguments(config, StartMode.Default)

            val mode = StartMode.valueOf(arguments[1])
            if (arguments.size == 2) return StartArguments(config, mode)

            val suiteId = arguments[2]
            val benchmarkId = arguments[3]
            return StartArguments(config, mode, suiteId, benchmarkId)
        }
    }

    fun toArray(): Array<out String> = when {
        mode == StartMode.Default -> arrayOf(config)
        suiteId == null || benchmarkId == null -> arrayOf(config, mode.name)
        else -> arrayOf(config, mode.name, suiteId, benchmarkId)
    }
}
package kotlinx.benchmark

internal fun String.replaceSpaceWithPercent() = replace(' ', '%')

internal const val resultTag = "<RESULT>"
internal const val endResultTag = "<ENDRESULT>"
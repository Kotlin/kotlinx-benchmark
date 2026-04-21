package kotlinx.benchmark.gradle.internal.android

import org.codehaus.groovy.ast.tools.GeneralUtils.args
import org.gradle.api.Task
import org.jetbrains.kotlin.com.intellij.ide.plugins.PluginManagerCore.logger
import java.io.File
import java.io.InputStream
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.sequences.forEach

// Trigger a Gradle task in the generated benchmark project
internal fun Task.runBenchmarkProjectGradleTask(
    projectDir: File,
    args: List<String>,
    showTaskOutputAsLifecycle: Boolean,
    timeoutMs: Long
): Pair<Boolean, Int> {

    val executeBenchmarkPath = projectDir.path
    val osName = System.getProperty("os.name").lowercase(Locale.ROOT)
    val gradlewPath = "$executeBenchmarkPath/gradlew" + if (osName.contains("win")) ".bat" else ""

    val modifiedArgs = listOf("-p", projectDir.path) + args
    logger.info("Running command: $gradlewPath ${modifiedArgs.joinToString(" ")}")

    val process = ProcessBuilder(gradlewPath, *modifiedArgs.toTypedArray())
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()

    val outputGobbler = StreamGobbler(process.inputStream) {
        // Almost all log output from the spawned Gradle process will be sent to the
        // normal input stream, regardless of log level. As we don't have a great way
        // to filter it on this side, we just send all of it to the info log, as it
        // can be helpful during debugging, but otherwise just pollute the main build log.
        when (showTaskOutputAsLifecycle) {
            true -> logger.lifecycle(it)
            false -> logger.info(it)
        }
    }
    val errorGobbler = StreamGobbler(process.errorStream) {
        logger.error(it)
    }
    outputGobbler.start()
    errorGobbler.start()

    val completed = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
    return completed to process.exitValue()
}


internal class StreamGobbler(private val inputStream: InputStream, private val consumer: (String) -> Unit) : Thread() {
    override fun run() {
        inputStream.bufferedReader().useLines { lines ->
            lines.forEach { consumer(it) }
        }
    }
}

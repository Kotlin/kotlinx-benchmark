package kotlinx.benchmark

import kotlin.js.Promise

private fun jsSpawnProcessAsync(
    childProcess: ChildProcess,
    binaryPath: String,
    workingDir: String,
    engineArguments: JsArray<JsString>,
    outputHandler: (String) -> Unit,
): Promise<JsNumber> = js("""{
    return new Promise((resolve, reject) => {
        const child = childProcess.spawn(binaryPath, engineArguments, { cwd: workingDir, stdio: ['inherit', 'pipe', 'inherit'] });
        child.stdout.setEncoding('utf8');
        child.stdout.on('data', outputHandler);
        child.on('close', resolve);
    });
}""")

internal fun spawnProcessAsyncAndProcessTags(binaryPath: String, workingDir: String, engineArguments: JsArray<JsString>, processResultTags: Boolean) {
    val stream = SplittedOutputStream(processResultTags)
    jsSpawnProcessAsync(childProcess, binaryPath, workingDir, engineArguments) {
        val trimmed = it.trimEnd()
        trimmed.forEach(stream::write)
        if (trimmed.length != it.length) {
            stream.flush()
        }
    }.then {
        stream.flush()
        it
    }
}

internal fun getJsParameters(engineArguments: List<String>?, modulePath: String, startArguments: StartArguments): JsArray<JsString> {
    val actualEngineArguments = engineArguments ?: listOf("<MODULE>", "<ARGUMENTS>")

    val jsArguments = JsArray<JsString>()
    var jsArgumentIndex = 0
    fun addJsArgument(argument: String) {
        jsArguments[jsArgumentIndex] = argument.toJsString()
        jsArgumentIndex++
    }

    actualEngineArguments.forEach { engineArgument ->
        when (engineArgument) {
            "<MODULE>" -> addJsArgument(modulePath)
            "<ARGUMENTS>" -> startArguments.toArray().forEach(::addJsArgument)
            else -> addJsArgument(engineArgument)
        }
    }
    return jsArguments
}

internal fun jsSpawnProcessWithExtraPipeSyncAndGetResult(
    childProcess: ChildProcess,
    binaryPath: String,
    workingDir: String,
    engineArguments: JsArray<JsString>,
): String? = js("""{
   const process = childProcess.spawnSync(binaryPath, engineArguments, { cwd: workingDir, encoding: 'utf8', stdio: ['inherit', 'inherit', 'inherit', 'pipe'] });
   return (process.status === 0) ? process.output[3] : null;
}""")
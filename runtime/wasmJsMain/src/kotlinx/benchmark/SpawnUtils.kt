package kotlinx.benchmark

import kotlin.js.Promise

private fun jsSpawnProcessAsync(
    childProcess: ChildProcess,
    binaryPath: JsString,
    workingDir: JsString,
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

internal fun spawnProcessAsyncAndProcessTags(binaryPath: JsString, workingDir: JsString, engineArguments: JsArray<JsString>, processResultTags: Boolean) {
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

internal val moduleMarker = "<MODULE>".toJsString()
internal val argumentsMarker = "<ARGUMENTS>".toJsString()

internal fun getJsParameters(engineArguments: JsArray<JsString>?, modulePath: JsString, startArguments: StartArguments): JsArray<JsString> {

    val jsArguments = JsArray<JsString>()
    var jsArgumentIndex = 0
    fun addJsArgument(argument: JsString) {
        jsArguments[jsArgumentIndex] = argument
        jsArgumentIndex++
    }

    fun handleArgument(engineArgument: JsString) {
        when (engineArgument) {
            moduleMarker -> addJsArgument(modulePath)
            argumentsMarker -> startArguments.toArray().forEach { x ->
                addJsArgument(x.toJsString())
            }
            else -> addJsArgument(engineArgument)
        }
    }

    if (engineArguments == null) {
        handleArgument(moduleMarker)
        handleArgument(argumentsMarker)
    } else {
        for (i in 0 until engineArguments.length) {
            handleArgument(engineArguments[i]!!)
        }
    }
    return jsArguments
}

internal fun jsSpawnProcessWithExtraPipeSyncAndGetResult(
    childProcess: ChildProcess,
    binaryPath: JsString,
    workingDir: JsString,
    engineArguments: JsArray<JsString>,
): String? = js("""{
   const process = childProcess.spawnSync(binaryPath, engineArguments, { cwd: workingDir, encoding: 'utf8', stdio: ['inherit', 'inherit', 'inherit', 'pipe'] });
   return (process.status === 0) ? process.output[3] : null;
}""")
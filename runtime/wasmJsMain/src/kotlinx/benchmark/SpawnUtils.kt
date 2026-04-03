package kotlinx.benchmark

import kotlin.js.Promise

private fun jsSpawnProcess(
    binaryPath: String,
    workingDir: String,
    engineArguments: JsArray<JsString>,
    outputHandler: (String) -> Unit,
): Promise<JsNumber> = js("""{
    const cp = require('child_process');
    return new Promise((resolve, reject) => {
        const child = cp.spawn(binaryPath, engineArguments, { cwd: workingDir, stdio: ['inherit', 'pipe', 'inherit'] });
        child.stdout.setEncoding('utf8');
        child.stdout.on('data', outputHandler);
        child.on('close', resolve);
    });
}""")

internal fun spawnProcessAndGetResult(binaryPath: String, workingDir: String, engineArguments: JsArray<JsString>): String? {
    var resultValue: String? = null
    jsSpawnProcess(binaryPath, workingDir, engineArguments) {
        val trimmed = it.trimEnd()
        val result = Regex("<RESULT>(.*)</RESULT>").find(trimmed)
        if (result != null) {
            resultValue = result.groupValues[1]
        } else {
            print(trimmed)
        }
    }.await()
    return resultValue?.takeIf { it.isNotEmpty() }
}

internal fun spawnProcess(binaryPath: String, workingDir: String, engineArguments: JsArray<JsString>) {
    val stream = ConsoleAndFilesOutputStream()
    jsSpawnProcess(binaryPath, workingDir, engineArguments) {
        val trimmed = it.trimEnd()
        trimmed.forEach(stream::write)
        if (trimmed.length != it.length) {
            stream.flush()
        }
    }.await()
    stream.flush()
}

internal fun getJsParameters(engineArguments: String?, modulePath: String, arguments: String): JsArray<JsString> {
    val actualEngineArguments = (engineArguments ?: "<MODULE> <ARGUMENTS>")
        .replace("<MODULE>", modulePath)
        .replace("<ARGUMENTS>", arguments)

    return actualEngineArguments.split(' ').toJsArray()
}

@JsFun("""(typeof WebAssembly.Suspending === 'undefined')
    ? () => { throw new Error('The node.js version is outdated. Please use version 25 or higher.'); }
    : new WebAssembly.Suspending((p) => p)""")
private external fun <T : JsAny> await(p: Promise<T>): T

private fun <T : JsAny> Promise<T>.await(): T = await(this)

@OptIn(ExperimentalJsExport::class)
@JsExport
private fun jsPromisingStart(body: JsReference<() -> Unit>): Unit = body.get().invoke()

private fun jsPromiseIntegration(body: JsReference<() -> Unit>): Unit =
    js("""(typeof WebAssembly.promising === 'undefined') ? wasmExports.jsPromisingStart(body) : WebAssembly.promising(wasmExports.jsPromisingStart)(body)""")

internal fun jsPromiseIntegration(body: () -> Unit) = jsPromiseIntegration(body.toJsReference())
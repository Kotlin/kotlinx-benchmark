package kotlinx.benchmark

@JsFun("(path, text) => require('fs').writeFileSync(path, text, 'utf8')")
private external fun nodeJsWriteFile(path: String, text: String)

@JsFun("(path) => require('fs').readFileSync(path, 'utf8')")
private external fun nodeJsReadFile(path: String): String

@JsFun("() => process.argv.slice(2).join(' ')")
private external fun nodeJsArguments(): String

internal object NodeJsEngineSupport : JsEngineSupport() {
    override fun writeFile(path: String, text: String) =
        nodeJsWriteFile(path, text)

    override fun readFile(path: String): String =
        nodeJsReadFile(path)

    override fun arguments(): Array<out String> =
        nodeJsArguments().split(' ').toTypedArray()
}
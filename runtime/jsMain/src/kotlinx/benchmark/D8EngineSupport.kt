package kotlinx.benchmark

private external fun read(path: String): String
private external fun write(text: String)

internal object D8EngineSupport : JsEngineSupport() {
    override fun writeFile(path: String, text: String) {
        //WORKAROUND: D8 cannot write into files, this format will be parsed on gradle plugin side
        write("<FILE:$path>$text<ENDFILE>")
    }

    override fun readFile(path: String): String =
        read(path)

    override fun arguments(): Array<out String> {
        val arguments = js("globalThis.arguments.join(' ')") as String
        return arguments.split(' ').toTypedArray()
    }
}
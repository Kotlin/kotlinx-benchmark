package kotlinx.benchmark

private external fun write(text: String)

internal object D8EngineSupport : StandaloneJsVmSupport() {
    override fun writeFile(path: String, text: String) {
        //WORKAROUND: In StandaloneJsVMs cannot write into files, this format will be parsed on gradle plugin side
        write("<FILE:$path>$text<ENDFILE>")
    }

    override fun arguments(): Array<out String> {
        val arguments = js("globalThis.arguments.join(' ')") as String
        return arguments.split(' ').toTypedArray()
    }
}

internal fun isD8Engine(): Boolean =
    js("typeof d8 !== 'undefined'") as Boolean